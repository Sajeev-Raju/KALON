package com.kalon.service;

import com.kalon.dto.*;
import com.kalon.entity.*;
import com.kalon.exception.InsufficientStockException;
import com.kalon.exception.OrderAccessException;
import com.kalon.exception.OrderStateException;
import com.kalon.exception.PaymentException;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.*;
import com.kalon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final RazorpayService razorpayService;
    private final ReturnRequestRepository returnRequestRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentMethodConfigService paymentMethodConfigService;
    private final EmailService emailService;
    private final StockMovementService stockMovementService;
    private final TaxService taxService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;

    // ────────────────────────────── Read operations ──────────────────────────────

    public List<OrderDTO> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Page<OrderDTO> getOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toDTO);
    }

    public OrderDTO getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessException("Order does not belong to user");
        }

        return toDTO(order);
    }

    public OrderDTO getOrderByNumber(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessException("Order does not belong to user");
        }

        return toDTO(order);
    }

    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toDTO);
    }

    public Page<OrderDTO> getAllOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable).map(this::toDTO);
    }

    public Page<OrderDTO> getOrdersWithFilters(Order.OrderStatus status,
                                                LocalDateTime dateFrom,
                                                LocalDateTime dateTo,
                                                BigDecimal minAmount,
                                                BigDecimal maxAmount,
                                                Pageable pageable) {
        return orderRepository.findWithFilters(status, dateFrom, dateTo, minAmount, maxAmount, pageable)
                .map(this::toDTO);
    }

    public OrderDTO getOrderByIdForAdmin(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toDTO(order);
    }

    // ────────────────────────────── COD order creation ──────────────────────────────

    @Transactional
    public OrderDTO createOrder(Long userId, CreateOrderRequest request) {
        log.info("Creating order: userId={}, addressId={}, paymentMethod={}", userId, request.getAddressId(), request.getPaymentMethod());

        // Validate payment method
        Order.PaymentMethod paymentMethod;
        try {
            paymentMethod = Order.PaymentMethod.valueOf(request.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            throw new OrderStateException("Invalid payment method: " + request.getPaymentMethod());
        }

        // Only COD is allowed through this endpoint
        if (paymentMethod != Order.PaymentMethod.COD) {
            throw new OrderStateException("Only COD orders can be placed through this endpoint. Use Razorpay endpoint for online payments.");
        }

        // Validate method is enabled via dynamic config
        if (!paymentMethodConfigService.isMethodEnabled(paymentMethod)) {
            throw new OrderStateException("Payment method '" + request.getPaymentMethod() + "' is not currently available");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new OrderStateException("Cart is empty");
        }

        Address address = validateAddress(request.getAddressId(), userId);

        ShippingAddress shippingAddress = buildShippingAddress(address);

        // Create order — COD orders start as PENDING and need admin confirmation
        Order order = Order.builder()
                .user(user)
                .subtotal(BigDecimal.ZERO)
                .shippingCost(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .shippingAddress(shippingAddress)
                .notes(request.getNotes())
                .build();

        order = orderRepository.save(order);

        // Create order items, validate stock, deduct stock, re-validate prices
        BigDecimal subtotal = createOrderItemsAndDeductStock(order, cart);

        // Calculate totals
        BigDecimal shippingCost = subtotal.compareTo(new BigDecimal("999")) >= 0 ?
                BigDecimal.ZERO : new BigDecimal("99");

        // Apply coupon discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            com.kalon.entity.Coupon coupon = couponService.findByCode(request.getCouponCode());
            couponService.validateAndCalculateDiscount(request.getCouponCode(), subtotal, userId);
            discountAmount = couponService.calculateDiscountAmount(coupon, subtotal);
            order.setCouponCode(coupon.getCode());
            order.setCouponId(coupon.getId());
            couponService.recordCouponUsage(coupon, user, order);
        }

        // Calculate tax (inclusive - GST is part of the product price)
        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxService.calculateInclusiveTax(taxableAmount);

        BigDecimal totalAmount = subtotal.add(shippingCost).subtract(discountAmount);

        order.setSubtotal(subtotal);
        order.setShippingCost(shippingCost);
        order.setDiscountAmount(discountAmount);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);

        // Validate amount limits for COD
        paymentMethodConfigService.validateMethodForAmount(paymentMethod, totalAmount);

        order = orderRepository.save(order);

        // Clear cart
        cartItemRepository.deleteByCartId(cart.getId());

        log.info("COD order created: orderNumber={}, userId={}, totalAmount={}", order.getOrderNumber(), userId, totalAmount);

        // Send order confirmation email (async)
        user.getEmail(); // Force lazy init before async call
        emailService.sendOrderConfirmationEmail(user, order);

        return toDTO(order);
    }

    // ────────────────────────────── Razorpay flow ──────────────────────────────

    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(Long userId, RazorpayOrderRequest request) {
        log.info("Creating Razorpay order: userId={}, addressId={}", userId, request.getAddressId());

        // Validate Razorpay is enabled
        if (!paymentMethodConfigService.isMethodEnabled(Order.PaymentMethod.RAZORPAY)) {
            throw new OrderStateException("Online payment is not currently available");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new OrderStateException("Cart is empty");
        }

        Address address = validateAddress(request.getAddressId(), userId);
        ShippingAddress shippingAddress = buildShippingAddress(address);

        // Create order
        Order order = Order.builder()
                .user(user)
                .subtotal(BigDecimal.ZERO)
                .shippingCost(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .paymentMethod(Order.PaymentMethod.RAZORPAY)
                .shippingAddress(shippingAddress)
                .notes(request.getNotes())
                .build();

        order = orderRepository.save(order);

        // Create order items, validate & deduct stock NOW (before payment)
        BigDecimal subtotal = createOrderItemsAndDeductStock(order, cart);

        BigDecimal shippingCost = subtotal.compareTo(new BigDecimal("999")) >= 0 ?
                BigDecimal.ZERO : new BigDecimal("99");

        // Apply coupon discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            com.kalon.entity.Coupon coupon = couponService.findByCode(request.getCouponCode());
            couponService.validateAndCalculateDiscount(request.getCouponCode(), subtotal, userId);
            discountAmount = couponService.calculateDiscountAmount(coupon, subtotal);
            order.setCouponCode(coupon.getCode());
            order.setCouponId(coupon.getId());
            couponService.recordCouponUsage(coupon, user, order);
        }

        // Calculate tax (inclusive - GST is part of the product price)
        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxService.calculateInclusiveTax(taxableAmount);

        BigDecimal totalAmount = subtotal.add(shippingCost).subtract(discountAmount);

        order.setSubtotal(subtotal);
        order.setShippingCost(shippingCost);
        order.setDiscountAmount(discountAmount);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        // Clear cart (stock is already reserved)
        cartItemRepository.deleteByCartId(cart.getId());

        // Create Razorpay order — if this fails, rollback stock and cancel the order
        String customerName = user.getFirstName() + " " + user.getLastName();
        RazorpayService.OrderResponse razorpayOrderResponse;
        try {
            razorpayOrderResponse = razorpayService.createOrder(
                    totalAmount,
                    order.getOrderNumber(),
                    customerName,
                    user.getEmail(),
                    user.getPhoneNumber()
            );
        } catch (PaymentException e) {
            log.error("Razorpay order creation failed, rolling back: orderNumber={}", order.getOrderNumber(), e);
            restoreStock(order);
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + "Payment gateway error: " + e.getMessage());
            orderRepository.save(order);
            throw new PaymentException("Payment gateway error. Please try again.");
        }

        order.setRazorpayOrderId(razorpayOrderResponse.getRazorpayOrderId());
        order = orderRepository.save(order);

        // Validate amount limits for Razorpay
        paymentMethodConfigService.validateMethodForAmount(Order.PaymentMethod.RAZORPAY, totalAmount);

        // Record payment attempt
        recordPaymentAttempt(order, razorpayOrderResponse.getRazorpayOrderId(),
                null, PaymentAttempt.AttemptStatus.INITIATED, PaymentAttempt.AttemptSource.CHECKOUT, null);

        Long amountInPaise = totalAmount.multiply(new BigDecimal("100")).longValue();

        log.info("Razorpay order created: orderNumber={}, userId={}, totalAmount={}, razorpayOrderId={}",
                order.getOrderNumber(), userId, totalAmount, razorpayOrderResponse.getRazorpayOrderId());

        return RazorpayOrderResponse.builder()
                .razorpayOrderId(razorpayOrderResponse.getRazorpayOrderId())
                .amount(razorpayOrderResponse.getAmount())
                .amountInPaise(amountInPaise)
                .currency(razorpayOrderResponse.getCurrency())
                .keyId(razorpayOrderResponse.getKeyId())
                .orderId(String.valueOf(order.getId()))
                .build();
    }

    @Transactional
    public OrderDTO verifyAndCompletePayment(Long userId, VerifyPaymentRequest request) {
        // Verify payment signature
        boolean isValid = razorpayService.verifyPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            log.error("Invalid payment signature: razorpayOrderId={}, userId={}", request.getRazorpayOrderId(), userId);
            throw new OrderStateException("Invalid payment signature");
        }

        // Find order by Razorpay order ID (with items eagerly loaded for toDTO)
        Order order = orderRepository.findByRazorpayOrderIdWithItems(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessException("Order does not belong to user");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED) {
            // Already completed — idempotent return
            return toDTO(order);
        }

        // Check if order has expired (30 minutes)
        if (order.getCreatedAt().plusMinutes(30).isBefore(LocalDateTime.now())) {
            log.warn("Payment attempt on expired order: orderNumber={}, createdAt={}",
                    order.getOrderNumber(), order.getCreatedAt());
            throw new OrderStateException("Order has expired. Please place a new order.");
        }

        // Verify payment amount matches order amount by fetching payment from Razorpay
        try {
            RazorpayService.PaymentDetails paymentDetails = razorpayService.fetchPayment(request.getRazorpayPaymentId());

            // Verify the payment's order_id matches our razorpay order id
            if (!request.getRazorpayOrderId().equals(paymentDetails.getOrderId())) {
                log.error("Payment order mismatch: expected={}, actual={}, paymentId={}",
                        request.getRazorpayOrderId(), paymentDetails.getOrderId(), request.getRazorpayPaymentId());
                throw new OrderStateException("Payment does not match order");
            }

            // Verify amount (in paise)
            long expectedAmountPaise = order.getTotalAmount().multiply(new BigDecimal("100")).longValue();
            if (!paymentDetails.getAmount().equals(expectedAmountPaise)) {
                log.error("Payment amount mismatch: orderNumber={}, expected={} paise, actual={} paise, paymentId={}",
                        order.getOrderNumber(), expectedAmountPaise, paymentDetails.getAmount(), request.getRazorpayPaymentId());
                throw new OrderStateException("Payment amount does not match order amount");
            }

            // Verify payment status is captured
            if (!"captured".equals(paymentDetails.getStatus())) {
                log.warn("Payment not captured: orderNumber={}, status={}, paymentId={}",
                        order.getOrderNumber(), paymentDetails.getStatus(), request.getRazorpayPaymentId());
                throw new OrderStateException("Payment not yet captured. Status: " + paymentDetails.getStatus());
            }

            log.info("Payment amount verified: orderNumber={}, amount={} paise", order.getOrderNumber(), expectedAmountPaise);
        } catch (PaymentException e) {
            // If we can't fetch payment details, log warning but still proceed
            // (signature was already verified, which is the primary security check)
            log.warn("Could not fetch payment details for amount verification: orderNumber={}, paymentId={}, error={}",
                    order.getOrderNumber(), request.getRazorpayPaymentId(), e.getMessage());
        }

        // Update payment status — stock was already deducted in createRazorpayOrder
        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setPaymentId(request.getRazorpayPaymentId());
        order = orderRepository.save(order);

        // Record successful payment attempt
        updatePaymentAttemptStatus(request.getRazorpayOrderId(), request.getRazorpayPaymentId(),
                PaymentAttempt.AttemptStatus.SUCCESS, null);

        log.info("Payment verified: orderNumber={}, userId={}, paymentId={}",
                order.getOrderNumber(), userId, request.getRazorpayPaymentId());

        // Send order confirmation email (async)
        order.getUser().getEmail(); // Force lazy init before async call
        emailService.sendOrderConfirmationEmail(order.getUser(), order);

        return toDTO(order);
    }

    // ────────────────────────────── Payment retry ──────────────────────────────

    private static final int MAX_PAYMENT_ATTEMPTS = 3;

    @Transactional
    public RazorpayOrderResponse retryPayment(Long userId, Long orderId) {
        log.info("Retrying payment: userId={}, orderId={}", userId, orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessException("Order does not belong to user");
        }

        // Only PENDING orders with PENDING payment can be retried
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new OrderStateException("Order is not in pending status. Current status: " + order.getStatus());
        }

        if (order.getPaymentStatus() != Order.PaymentStatus.PENDING) {
            throw new OrderStateException("Payment is not in pending status. Current status: " + order.getPaymentStatus());
        }

        if (order.getPaymentMethod() != Order.PaymentMethod.RAZORPAY) {
            throw new OrderStateException("Payment retry is only available for online payment orders");
        }

        // Check if order has expired (30 minutes from creation)
        if (order.getCreatedAt().plusMinutes(30).isBefore(LocalDateTime.now())) {
            throw new OrderStateException("Order has expired. Please place a new order.");
        }

        // Count attempts from the payment_attempts table
        long attemptCount = paymentAttemptRepository.countByOrderId(order.getId());
        if (attemptCount >= MAX_PAYMENT_ATTEMPTS) {
            throw new OrderStateException("Maximum payment attempts (" + MAX_PAYMENT_ATTEMPTS + ") reached. Please place a new order.");
        }

        User user = order.getUser();
        String customerName = user.getFirstName() + " " + user.getLastName();

        // Create a new Razorpay order
        RazorpayService.OrderResponse razorpayOrderResponse;
        try {
            razorpayOrderResponse = razorpayService.createOrder(
                    order.getTotalAmount(),
                    order.getOrderNumber(),
                    customerName,
                    user.getEmail(),
                    user.getPhoneNumber()
            );
        } catch (PaymentException e) {
            log.error("Razorpay retry failed: orderNumber={}", order.getOrderNumber(), e);
            throw new PaymentException("Payment gateway error. Please try again.");
        }

        // Update the razorpay order ID to the new one
        order.setRazorpayOrderId(razorpayOrderResponse.getRazorpayOrderId());
        // Reset payment status to PENDING for retry (in case webhook set it to FAILED)
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        orderRepository.save(order);

        // Record retry attempt
        recordPaymentAttempt(order, razorpayOrderResponse.getRazorpayOrderId(),
                null, PaymentAttempt.AttemptStatus.INITIATED, PaymentAttempt.AttemptSource.RETRY, null);

        Long amountInPaise = order.getTotalAmount().multiply(new BigDecimal("100")).longValue();

        log.info("Payment retry: orderNumber={}, attempt={}, newRazorpayOrderId={}",
                order.getOrderNumber(), attemptCount + 1, razorpayOrderResponse.getRazorpayOrderId());

        return RazorpayOrderResponse.builder()
                .razorpayOrderId(razorpayOrderResponse.getRazorpayOrderId())
                .amount(razorpayOrderResponse.getAmount())
                .amountInPaise(amountInPaise)
                .currency(razorpayOrderResponse.getCurrency())
                .keyId(razorpayOrderResponse.getKeyId())
                .orderId(String.valueOf(order.getId()))
                .build();
    }

    // ────────────────────────────── Webhook handling ──────────────────────────────

    @Transactional
    public void handlePaymentCaptured(String razorpayPaymentId, String razorpayOrderId, Long amountPaise) {
        log.info("Webhook: payment.captured — razorpayOrderId={}, paymentId={}, amount={}",
                razorpayOrderId, razorpayPaymentId, amountPaise);

        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (order == null) {
            log.warn("Webhook: order not found for razorpayOrderId={}", razorpayOrderId);
            return;
        }

        // Idempotent — skip if already completed
        if (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED) {
            log.info("Webhook: payment already completed for orderNumber={}", order.getOrderNumber());
            return;
        }

        // Verify amount
        long expectedPaise = order.getTotalAmount().multiply(new BigDecimal("100")).longValue();
        if (!Long.valueOf(expectedPaise).equals(amountPaise)) {
            log.error("Webhook: amount mismatch for orderNumber={}. Expected={}, received={}",
                    order.getOrderNumber(), expectedPaise, amountPaise);
            return;
        }

        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setPaymentId(razorpayPaymentId);
        orderRepository.save(order);

        // Record webhook success
        updatePaymentAttemptStatus(razorpayOrderId, razorpayPaymentId,
                PaymentAttempt.AttemptStatus.SUCCESS, null);

        log.info("Webhook: order confirmed — orderNumber={}, paymentId={}", order.getOrderNumber(), razorpayPaymentId);
    }

    @Transactional
    public void handlePaymentFailed(String razorpayOrderId, String razorpayPaymentId) {
        log.info("Webhook: payment.failed — razorpayOrderId={}, paymentId={}", razorpayOrderId, razorpayPaymentId);

        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (order == null) {
            log.warn("Webhook: order not found for razorpayOrderId={}", razorpayOrderId);
            return;
        }

        // Only update if still pending — don't override COMPLETED/REFUNDED
        if (order.getPaymentStatus() != Order.PaymentStatus.PENDING) {
            log.info("Webhook: skipping — payment status is {} for orderNumber={}", order.getPaymentStatus(), order.getOrderNumber());
            return;
        }

        order.setPaymentStatus(Order.PaymentStatus.FAILED);
        orderRepository.save(order);

        // Record webhook failure
        updatePaymentAttemptStatus(razorpayOrderId, razorpayPaymentId,
                PaymentAttempt.AttemptStatus.FAILED, "Payment failed (webhook)");

        log.info("Webhook: payment failed recorded — orderNumber={}", order.getOrderNumber());
    }

    // ────────────────────────────── Status management ──────────────────────────────

    private static final java.util.Map<Order.OrderStatus, java.util.Set<Order.OrderStatus>> VALID_TRANSITIONS;
    static {
        VALID_TRANSITIONS = new java.util.EnumMap<>(Order.OrderStatus.class);
        VALID_TRANSITIONS.put(Order.OrderStatus.PENDING,
                java.util.EnumSet.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(Order.OrderStatus.CONFIRMED,
                java.util.EnumSet.of(Order.OrderStatus.PROCESSING, Order.OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(Order.OrderStatus.PROCESSING,
                java.util.EnumSet.of(Order.OrderStatus.SHIPPED, Order.OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(Order.OrderStatus.SHIPPED,
                java.util.EnumSet.of(Order.OrderStatus.DELIVERED));
        VALID_TRANSITIONS.put(Order.OrderStatus.DELIVERED,
                java.util.EnumSet.of(Order.OrderStatus.RETURNED));
        VALID_TRANSITIONS.put(Order.OrderStatus.CANCELLED, java.util.EnumSet.noneOf(Order.OrderStatus.class));
        VALID_TRANSITIONS.put(Order.OrderStatus.RETURNED, java.util.EnumSet.noneOf(Order.OrderStatus.class));
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
        Order.OrderStatus currentStatus = order.getStatus();

        log.info("Updating order status: orderId={}, orderNumber={}, {} -> {}",
                orderId, order.getOrderNumber(), currentStatus, newStatus);

        java.util.Set<Order.OrderStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new OrderStateException("Invalid status transition: " + currentStatus + " → " + newStatus);
        }

        order.setStatus(newStatus);
        order = orderRepository.save(order);

        // Send email notifications for status changes
        if (newStatus == Order.OrderStatus.SHIPPED) {
            order.getUser().getEmail(); // Force lazy init before async call
            emailService.sendShippingNotificationEmail(order.getUser(), order);
        } else if (newStatus == Order.OrderStatus.DELIVERED) {
            order.getUser().getEmail(); // Force lazy init before async call
            emailService.sendOrderDeliveredEmail(order.getUser(), order);
        }

        log.info("Order status updated: orderNumber={}, newStatus={}", order.getOrderNumber(), newStatus);
        return toDTO(order);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId, Long userId) {
        log.info("Cancelling order: orderId={}, userId={}", orderId, userId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessException("Order does not belong to user");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new OrderStateException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        // Restore stock with pessimistic locking
        restoreStock(order);

        order.setStatus(Order.OrderStatus.CANCELLED);

        // Handle refund based on payment method
        if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
            // COD — no online payment was made, no refund needed
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
        } else if (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED
                && order.getPaymentId() != null && !order.getPaymentId().isEmpty()) {
            // Online payment was completed — initiate refund
            try {
                razorpayService.createRefund(
                        order.getPaymentId(),
                        order.getTotalAmount(),
                        "Order cancelled: " + order.getOrderNumber()
                );
                order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            } catch (Exception e) {
                log.error("Failed to create Razorpay refund for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
                // Keep as COMPLETED — admin must retry refund manually
                // Do NOT mark as REFUNDED when refund actually failed
                order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "")
                        + "Refund failed: " + e.getMessage());
            }
        } else {
            // Online payment was pending (not yet paid) — just mark as failed
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
        }

        order = orderRepository.save(order);

        // Send cancellation email (async)
        order.getUser().getEmail(); // Force lazy init before async call
        emailService.sendOrderCancellationEmail(order.getUser(), order);

        log.info("Order cancelled: orderNumber={}, userId={}, paymentStatus={}",
                order.getOrderNumber(), userId, order.getPaymentStatus());
        return toDTO(order);
    }

    // ────────────────────────────── Orphaned order cleanup ──────────────────────────────

    @Scheduled(fixedRate = 900000) // Every 15 minutes
    @Transactional
    public void cleanupExpiredPendingOrders() {
        LocalDateTime expiry = LocalDateTime.now().minusMinutes(30);
        List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(
                Order.OrderStatus.PENDING, Order.PaymentStatus.PENDING, expiry);

        for (Order order : expiredOrders) {
            try {
                restoreStock(order);
                order.setStatus(Order.OrderStatus.CANCELLED);
                order.setPaymentStatus(Order.PaymentStatus.FAILED);
                order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + "Auto-cancelled: payment timeout");
                orderRepository.save(order);
                log.info("Auto-cancelled expired pending order: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to cleanup expired order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            }
        }
    }

    // ────────────────────────────── Payment attempt tracking ──────────────────────────────

    public List<PaymentAttemptDTO> getPaymentAttempts(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessException("Order does not belong to user");
        }

        return paymentAttemptRepository.findByOrderIdOrderByAttemptNumberDesc(orderId)
                .stream().map(this::toPaymentAttemptDTO).collect(Collectors.toList());
    }

    public List<PaymentAttemptDTO> getPaymentAttemptsForAdmin(Long orderId) {
        return paymentAttemptRepository.findByOrderIdOrderByAttemptNumberDesc(orderId)
                .stream().map(this::toPaymentAttemptDTO).collect(Collectors.toList());
    }

    private void recordPaymentAttempt(Order order, String razorpayOrderId, String razorpayPaymentId,
                                       PaymentAttempt.AttemptStatus status, PaymentAttempt.AttemptSource source,
                                       String failureReason) {
        int attemptNumber = paymentAttemptRepository.findMaxAttemptNumberByOrderId(order.getId())
                .map(n -> n + 1).orElse(1);

        PaymentAttempt attempt = PaymentAttempt.builder()
                .order(order)
                .attemptNumber(attemptNumber)
                .razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId(razorpayPaymentId)
                .amount(order.getTotalAmount())
                .status(status)
                .failureReason(failureReason)
                .source(source)
                .build();

        paymentAttemptRepository.save(attempt);
        log.debug("Payment attempt recorded: orderId={}, attempt={}, status={}, source={}",
                order.getId(), attemptNumber, status, source);
    }

    private void updatePaymentAttemptStatus(String razorpayOrderId, String razorpayPaymentId,
                                             PaymentAttempt.AttemptStatus status, String failureReason) {
        PaymentAttempt attempt = paymentAttemptRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);

        if (attempt != null) {
            attempt.setRazorpayPaymentId(razorpayPaymentId);
            attempt.setStatus(status);
            if (failureReason != null) {
                attempt.setFailureReason(failureReason);
            }
            paymentAttemptRepository.save(attempt);
        }
    }

    private PaymentAttemptDTO toPaymentAttemptDTO(PaymentAttempt attempt) {
        return PaymentAttemptDTO.builder()
                .id(attempt.getId())
                .orderId(attempt.getOrder().getId())
                .attemptNumber(attempt.getAttemptNumber())
                .razorpayOrderId(attempt.getRazorpayOrderId())
                .razorpayPaymentId(attempt.getRazorpayPaymentId())
                .amount(attempt.getAmount())
                .status(attempt.getStatus().name())
                .failureReason(attempt.getFailureReason())
                .paymentMethod(attempt.getPaymentMethod())
                .source(attempt.getSource().name())
                .createdAt(attempt.getCreatedAt())
                .build();
    }

    // ────────────────────────────── Private helpers ──────────────────────────────

    private Address validateAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new OrderAccessException("Address does not belong to user");
        }

        return address;
    }

    private ShippingAddress buildShippingAddress(Address address) {
        return ShippingAddress.builder()
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }

    /**
     * Creates order items from cart, validates and deducts stock with pessimistic locking,
     * and re-validates prices against current product data.
     * Returns the computed subtotal.
     */
    private BigDecimal createOrderItemsAndDeductStock(Order order, Cart cart) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            // Validate product is still active
            if (!cartItem.getProduct().isActive()) {
                log.warn("Attempted to order inactive product: productId={}, productName={}, orderId={}",
                        cartItem.getProduct().getId(), cartItem.getProduct().getName(), order.getId());
                throw new OrderStateException("Product " + cartItem.getProduct().getName() + " is no longer available");
            }

            // Re-validate price against current product price
            BigDecimal currentPrice = cartItem.getProduct().getPrice();

            // Get primary image
            String primaryImageUrl = cartItem.getProduct().getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(cartItem.getProduct().getImages().isEmpty() ? null :
                            cartItem.getProduct().getImages().get(0).getImageUrl());

            // Validate and deduct stock with pessimistic lock
            ProductVariant variant = productVariantRepository.findByIdForUpdate(cartItem.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

            if (!variant.isAvailable() || variant.getStockQuantity() < cartItem.getQuantity()) {
                log.warn("Insufficient stock during order creation: variantId={}, requested={}, available={}, orderId={}",
                        variant.getId(), cartItem.getQuantity(), variant.getStockQuantity(), order.getId());
                throw new InsufficientStockException(
                        variant.getStockQuantity(),
                        0,
                        variant.getStockQuantity(),
                        cartItem.getQuantity()
                );
            }

            int quantityBefore = variant.getStockQuantity();
            variant.setStockQuantity(quantityBefore - cartItem.getQuantity());
            productVariantRepository.save(variant);

            stockMovementService.recordMovement(variant, cartItem.getProduct(),
                    com.kalon.entity.StockMovement.MovementType.ORDER_PLACED,
                    -cartItem.getQuantity(), quantityBefore, variant.getStockQuantity(),
                    com.kalon.entity.StockMovement.ReferenceType.ORDER, order.getId(),
                    "Order " + order.getOrderNumber());

            BigDecimal itemTotal = currentPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .variantId(cartItem.getVariant().getId())
                    .productName(cartItem.getProduct().getName())
                    .productImage(primaryImageUrl)
                    .size(cartItem.getVariant().getSize())
                    .color(cartItem.getVariant().getColor())
                    .quantity(cartItem.getQuantity())
                    .price(currentPrice)
                    .totalPrice(itemTotal)
                    .build();

            order.getItems().add(orderItem);
            subtotal = subtotal.add(itemTotal);
        }

        return subtotal;
    }

    /**
     * Restores stock for all items in an order using pessimistic locking.
     * Uses variantId when available, falls back to product+size+color lookup.
     */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = null;

            // Prefer direct variantId lookup
            if (item.getVariantId() != null) {
                variant = productVariantRepository.findByIdForUpdate(item.getVariantId())
                        .orElse(null);
            }

            // Fallback to product+size+color lookup
            if (variant == null) {
                variant = productVariantRepository.findByProductIdAndSizeAndColor(
                        item.getProduct().getId(),
                        item.getSize(),
                        item.getColor()
                ).orElse(null);
            }

            if (variant != null) {
                int quantityBefore = variant.getStockQuantity();
                variant.setStockQuantity(quantityBefore + item.getQuantity());
                productVariantRepository.save(variant);

                stockMovementService.recordMovement(variant, item.getProduct(),
                        com.kalon.entity.StockMovement.MovementType.ORDER_CANCELLED,
                        item.getQuantity(), quantityBefore, variant.getStockQuantity(),
                        com.kalon.entity.StockMovement.ReferenceType.ORDER, order.getId(),
                        "Order cancelled: " + order.getOrderNumber());

                log.debug("Stock restored: variantId={}, quantity={}, orderNumber={}",
                        variant.getId(), item.getQuantity(), order.getOrderNumber());
            } else {
                log.warn("Could not find variant to restore stock: orderNumber={}, orderItemId={}, productId={}, size={}, color={}",
                        order.getOrderNumber(), item.getId(), item.getProduct().getId(), item.getSize(), item.getColor());
            }
        }
    }

    // ────────────────────────────── DTO mapping ──────────────────────────────

    private OrderDTO toDTO(Order order) {
        // Batch-load return request status for all items (fix N+1)
        List<Long> orderItemIds = order.getItems().stream()
                .map(OrderItem::getId)
                .collect(Collectors.toList());

        Set<Long> returnRequestedItemIds = orderItemIds.isEmpty()
                ? Set.of()
                : returnRequestRepository.findOrderItemIdsWithReturnRequests(orderItemIds)
                        .stream().collect(Collectors.toSet());

        List<OrderItemDTO> items = order.getItems().stream()
                .map(item -> itemToDTO(item, returnRequestedItemIds.contains(item.getId())))
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .subtotal(order.getSubtotal())
                .shippingCost(order.getShippingCost())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentId(order.getPaymentId())
                .razorpayOrderId(order.getRazorpayOrderId())
                .shippingAddress(shippingAddressToDTO(order.getShippingAddress()))
                .trackingNumber(order.getTrackingNumber())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    private OrderItemDTO itemToDTO(OrderItem item, boolean hasReturnRequest) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .size(item.getSize())
                .color(item.getColor())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(item.getTotalPrice())
                .isReturnable(item.getProduct().isReturnable())
                .returnRequested(hasReturnRequest)
                .build();
    }

    private AddressDTO shippingAddressToDTO(ShippingAddress sa) {
        if (sa == null) {
            return null;
        }
        return AddressDTO.builder()
                .fullName(sa.getFullName())
                .phoneNumber(sa.getPhoneNumber())
                .addressLine1(sa.getAddressLine1())
                .addressLine2(sa.getAddressLine2())
                .city(sa.getCity())
                .state(sa.getState())
                .postalCode(sa.getPostalCode())
                .country(sa.getCountry())
                .build();
    }
}
