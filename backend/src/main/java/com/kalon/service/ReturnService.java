package com.kalon.service;

import com.kalon.dto.*;
import com.kalon.entity.*;
import com.kalon.exception.InsufficientStockException;
import com.kalon.exception.OrderAccessException;
import com.kalon.exception.OrderStateException;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService {

    private static final int RETURN_WINDOW_DAYS = 30;

    // ────────────────────── Status Transition Map ──────────────────────
    private static final Map<ReturnRequest.ReturnStatus, Set<ReturnRequest.ReturnStatus>> VALID_TRANSITIONS;

    static {
        Map<ReturnRequest.ReturnStatus, Set<ReturnRequest.ReturnStatus>> map = new EnumMap<>(ReturnRequest.ReturnStatus.class);
        map.put(ReturnRequest.ReturnStatus.PENDING,
                EnumSet.of(ReturnRequest.ReturnStatus.APPROVED, ReturnRequest.ReturnStatus.REJECTED, ReturnRequest.ReturnStatus.CANCELLED));
        map.put(ReturnRequest.ReturnStatus.APPROVED,
                EnumSet.of(ReturnRequest.ReturnStatus.PROCESSING, ReturnRequest.ReturnStatus.CANCELLED));
        map.put(ReturnRequest.ReturnStatus.PROCESSING,
                EnumSet.of(ReturnRequest.ReturnStatus.COMPLETED, ReturnRequest.ReturnStatus.CANCELLED));
        map.put(ReturnRequest.ReturnStatus.REJECTED, EnumSet.noneOf(ReturnRequest.ReturnStatus.class));
        map.put(ReturnRequest.ReturnStatus.COMPLETED, EnumSet.noneOf(ReturnRequest.ReturnStatus.class));
        map.put(ReturnRequest.ReturnStatus.CANCELLED, EnumSet.noneOf(ReturnRequest.ReturnStatus.class));
        VALID_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final RazorpayService razorpayService;
    private final EmailService emailService;
    private final StockMovementService stockMovementService;

    // ────────────────────── User Endpoints ──────────────────────

    @Transactional
    public ReturnRequestDTO createReturnRequest(Long userId, CreateReturnRequestDTO request) {
        log.info("Creating return request: userId={}, orderItemId={}", userId, request.getOrderItemId());

        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        Order order = orderItem.getOrder();
        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessException("Order does not belong to user");
        }

        if (!orderItem.getProduct().isReturnable()) {
            throw new OrderStateException("Returns are not allowed for this product");
        }

        if (order.getStatus() != Order.OrderStatus.DELIVERED && order.getStatus() != Order.OrderStatus.SHIPPED) {
            throw new OrderStateException("Order must be delivered or shipped to request a return");
        }

        // Enforce 30-day return window
        if (order.getStatus() == Order.OrderStatus.DELIVERED && order.getUpdatedAt() != null) {
            long daysSinceDelivery = ChronoUnit.DAYS.between(order.getUpdatedAt(), LocalDateTime.now());
            if (daysSinceDelivery > RETURN_WINDOW_DAYS) {
                throw new OrderStateException(
                        "Return window has expired. Returns must be initiated within " + RETURN_WINDOW_DAYS + " days of delivery.");
            }
        }

        if (returnRequestRepository.findByOrderItemId(orderItem.getId()).isPresent()) {
            throw new OrderStateException("Return request already exists for this item");
        }

        ReturnRequest.RequestType requestType = request.getRequestType();

        // Validate exchange — check product/variant exist and have stock
        if (requestType == ReturnRequest.RequestType.EXCHANGE) {
            if (request.getExchangeProductId() == null || request.getExchangeVariantId() == null) {
                throw new OrderStateException("Exchange product and variant must be specified");
            }

            Product exchangeProduct = productRepository.findById(request.getExchangeProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Exchange product not found"));

            if (!exchangeProduct.isActive()) {
                throw new OrderStateException("Exchange product is no longer available");
            }

            ProductVariant exchangeVariant = productVariantRepository.findById(request.getExchangeVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Exchange variant not found"));

            if (!exchangeVariant.getProduct().getId().equals(request.getExchangeProductId())) {
                throw new OrderStateException("Exchange variant does not belong to the selected product");
            }

            if (!exchangeVariant.isAvailable() || exchangeVariant.getStockQuantity() < orderItem.getQuantity()) {
                throw new InsufficientStockException(
                        exchangeVariant.getStockQuantity(), 0,
                        exchangeVariant.getStockQuantity(), orderItem.getQuantity());
            }
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .order(order)
                .orderItem(orderItem)
                .requestType(requestType)
                .status(ReturnRequest.ReturnStatus.PENDING)
                .reason(request.getReason())
                .refundAmount(orderItem.getTotalPrice())
                .exchangeProductId(request.getExchangeProductId())
                .exchangeVariantId(request.getExchangeVariantId())
                .refundStatus(requestType == ReturnRequest.RequestType.RETURN
                        ? ReturnRequest.RefundStatus.PENDING : null)
                .build();

        returnRequest = returnRequestRepository.save(returnRequest);

        log.info("Return request created: returnRequestId={}, orderNumber={}, type={}",
                returnRequest.getId(), order.getOrderNumber(), requestType);
        return toDTO(returnRequest);
    }

    public List<ReturnRequestDTO> getReturnRequestsByUserId(Long userId) {
        return returnRequestRepository.findByOrderUserId(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ReturnRequestDTO getReturnRequestById(Long returnRequestId, Long userId) {
        ReturnRequest returnRequest = returnRequestRepository.findByIdWithDetails(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        if (!returnRequest.getOrder().getUser().getId().equals(userId)) {
            throw new OrderAccessException("Return request does not belong to user");
        }

        return toDTO(returnRequest);
    }

    @Transactional
    public ReturnRequestDTO cancelReturnRequest(Long returnRequestId, Long userId) {
        log.info("Cancelling return request: returnRequestId={}, userId={}", returnRequestId, userId);

        ReturnRequest returnRequest = returnRequestRepository.findByIdWithDetails(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        if (!returnRequest.getOrder().getUser().getId().equals(userId)) {
            throw new OrderAccessException("Return request does not belong to user");
        }

        validateTransition(returnRequest.getStatus(), ReturnRequest.ReturnStatus.CANCELLED);

        // Users can only cancel PENDING requests
        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.PENDING) {
            throw new OrderStateException("Only pending return requests can be cancelled by user");
        }

        returnRequest.setStatus(ReturnRequest.ReturnStatus.CANCELLED);
        returnRequest = returnRequestRepository.save(returnRequest);

        log.info("Return request cancelled by user: returnRequestId={}, orderNumber={}",
                returnRequestId, returnRequest.getOrder().getOrderNumber());
        return toDTO(returnRequest);
    }

    // ────────────────────── Admin Query Endpoints ──────────────────────

    public Page<ReturnRequestDTO> getAllReturnRequests(Pageable pageable) {
        return returnRequestRepository.findAllWithDetails(pageable).map(this::toDTO);
    }

    public Page<ReturnRequestDTO> searchReturnRequests(String search, Pageable pageable) {
        return returnRequestRepository.searchReturnRequests(search, pageable).map(this::toDTO);
    }

    public Page<ReturnRequestDTO> getReturnRequestsByStatus(ReturnRequest.ReturnStatus status, Pageable pageable) {
        return returnRequestRepository.findByStatus(status, pageable).map(this::toDTO);
    }

    public Page<ReturnRequestDTO> getReturnRequestsByType(ReturnRequest.RequestType type, Pageable pageable) {
        return returnRequestRepository.findByRequestType(type, pageable).map(this::toDTO);
    }

    public Page<ReturnRequestDTO> getReturnRequestsByStatusAndType(
            ReturnRequest.ReturnStatus status, ReturnRequest.RequestType type, Pageable pageable) {
        return returnRequestRepository.findByStatusAndRequestType(status, type, pageable).map(this::toDTO);
    }

    public ReturnRequestDTO getReturnRequestByIdForAdmin(Long returnRequestId) {
        ReturnRequest returnRequest = returnRequestRepository.findByIdWithDetails(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));
        return toDTO(returnRequest);
    }

    // ────────────────────── Admin State Transitions ──────────────────────

    @Transactional
    public ReturnRequestDTO approveReturnRequest(Long returnRequestId, String adminNotes) {
        log.info("Approving return request: returnRequestId={}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findByIdWithDetails(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        validateTransition(returnRequest.getStatus(), ReturnRequest.ReturnStatus.APPROVED);

        returnRequest.setAdminNotes(adminNotes);
        returnRequest.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        returnRequest = returnRequestRepository.save(returnRequest);

        // Send return approved email (async)
        Order approvedOrder = returnRequest.getOrder();
        User approvedUser = approvedOrder.getUser();
        approvedUser.getEmail(); // Force lazy init
        emailService.sendReturnStatusEmail(approvedUser, approvedOrder, returnRequest, "Approved");

        log.info("Return request approved: returnRequestId={}, orderNumber={}",
                returnRequestId, approvedOrder.getOrderNumber());
        return toDTO(returnRequest);
    }

    @Transactional
    public ReturnRequestDTO processReturnRequest(Long returnRequestId, String adminNotes) {
        log.info("Processing return request: returnRequestId={}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findByIdWithDetails(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        validateTransition(returnRequest.getStatus(), ReturnRequest.ReturnStatus.PROCESSING);

        Order order = returnRequest.getOrder();

        // Initiate refund for RETURN type
        if (returnRequest.getRequestType() == ReturnRequest.RequestType.RETURN) {
            initiateRefund(returnRequest, order);
        }

        // Reserve exchange stock with pessimistic locking
        if (returnRequest.getRequestType() == ReturnRequest.RequestType.EXCHANGE) {
            reserveExchangeStock(returnRequest);
        }

        // NOTE: Returned item stock is restored in completeReturnRequest (item must be received first)

        if (adminNotes != null && !adminNotes.isBlank()) {
            returnRequest.setAdminNotes(adminNotes);
        }

        returnRequest.setStatus(ReturnRequest.ReturnStatus.PROCESSING);
        returnRequest = returnRequestRepository.save(returnRequest);

        // Send return processing email (async)
        order.getUser().getEmail(); // Force lazy init
        emailService.sendReturnStatusEmail(order.getUser(), order, returnRequest, "Processing");

        log.info("Return request processing: returnRequestId={}, orderNumber={}",
                returnRequestId, order.getOrderNumber());
        return toDTO(returnRequest);
    }

    @Transactional
    public ReturnRequestDTO rejectReturnRequest(Long returnRequestId, String adminNotes) {
        log.info("Rejecting return request: returnRequestId={}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findByIdWithDetails(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        validateTransition(returnRequest.getStatus(), ReturnRequest.ReturnStatus.REJECTED);

        returnRequest.setStatus(ReturnRequest.ReturnStatus.REJECTED);
        returnRequest.setAdminNotes(adminNotes);
        returnRequest = returnRequestRepository.save(returnRequest);

        // Send return rejected email (async)
        Order rejectedOrder = returnRequest.getOrder();
        User rejectedUser = rejectedOrder.getUser();
        rejectedUser.getEmail(); // Force lazy init
        emailService.sendReturnStatusEmail(rejectedUser, rejectedOrder, returnRequest, "Rejected");

        log.info("Return request rejected: returnRequestId={}, orderNumber={}",
                returnRequestId, rejectedOrder.getOrderNumber());
        return toDTO(returnRequest);
    }

    @Transactional
    public ReturnRequestDTO completeReturnRequest(Long returnRequestId, String adminNotes) {
        log.info("Completing return request: returnRequestId={}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findByIdWithDetails(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        validateTransition(returnRequest.getStatus(), ReturnRequest.ReturnStatus.COMPLETED);

        returnRequest.setStatus(ReturnRequest.ReturnStatus.COMPLETED);

        // Only mark refund completed if it was processing (preserve FAILED)
        if (returnRequest.getRequestType() == ReturnRequest.RequestType.RETURN) {
            if (returnRequest.getRefundStatus() == ReturnRequest.RefundStatus.PROCESSING) {
                returnRequest.setRefundStatus(ReturnRequest.RefundStatus.COMPLETED);
            }
        }

        // Restore returned item stock with pessimistic locking
        restoreReturnedItemStock(returnRequest);

        if (adminNotes != null && !adminNotes.isBlank()) {
            returnRequest.setAdminNotes(adminNotes);
        }

        // Check if all items are returned → mark order as RETURNED
        Order order = returnRequest.getOrder();
        long totalReturnedItems = returnRequestRepository.findByOrderId(order.getId())
                .stream()
                .filter(rr -> rr.getStatus() == ReturnRequest.ReturnStatus.COMPLETED)
                .count();

        if (totalReturnedItems >= order.getItems().size()) {
            order.setStatus(Order.OrderStatus.RETURNED);
            orderRepository.save(order);
            log.info("All items returned — order marked as RETURNED: orderNumber={}", order.getOrderNumber());
        }

        returnRequest = returnRequestRepository.save(returnRequest);

        // Send return completed email (async)
        order.getUser().getEmail(); // Force lazy init
        emailService.sendReturnStatusEmail(order.getUser(), order, returnRequest, "Completed");

        log.info("Return request completed: returnRequestId={}, orderNumber={}",
                returnRequestId, order.getOrderNumber());
        return toDTO(returnRequest);
    }

    @Transactional
    public ReturnRequestDTO adminCancelReturnRequest(Long returnRequestId, String adminNotes) {
        log.info("Admin cancelling return request: returnRequestId={}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findByIdWithDetails(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        validateTransition(returnRequest.getStatus(), ReturnRequest.ReturnStatus.CANCELLED);

        // If PROCESSING exchange, restore reserved stock with pessimistic locking
        if (returnRequest.getStatus() == ReturnRequest.ReturnStatus.PROCESSING
                && returnRequest.getRequestType() == ReturnRequest.RequestType.EXCHANGE
                && returnRequest.getExchangeVariantId() != null) {
            int restoreQuantity = returnRequest.getOrderItem().getQuantity();
            ProductVariant exchangeVariant = productVariantRepository.findByIdForUpdate(returnRequest.getExchangeVariantId())
                    .orElse(null);
            if (exchangeVariant != null) {
                int quantityBefore = exchangeVariant.getStockQuantity();
                exchangeVariant.setStockQuantity(quantityBefore + restoreQuantity);
                productVariantRepository.save(exchangeVariant);

                stockMovementService.recordMovement(exchangeVariant,
                        returnRequest.getOrderItem().getProduct(),
                        com.kalon.entity.StockMovement.MovementType.EXCHANGE_CANCELLED,
                        restoreQuantity, quantityBefore, exchangeVariant.getStockQuantity(),
                        com.kalon.entity.StockMovement.ReferenceType.RETURN_REQUEST, returnRequest.getId(),
                        "Exchange stock restored on return cancellation");

                log.info("Exchange stock restored on cancel: exchangeVariantId={}, quantity={}",
                        returnRequest.getExchangeVariantId(), restoreQuantity);
            }
        }

        returnRequest.setStatus(ReturnRequest.ReturnStatus.CANCELLED);
        if (adminNotes != null && !adminNotes.isBlank()) {
            returnRequest.setAdminNotes(adminNotes);
        }
        returnRequest = returnRequestRepository.save(returnRequest);

        log.info("Return request admin-cancelled: returnRequestId={}", returnRequestId);
        return toDTO(returnRequest);
    }

    // ────────────────────── Refund Retry ──────────────────────

    @Transactional
    public ReturnRequestDTO retryRefund(Long returnRequestId) {
        log.info("Retrying refund for return request: returnRequestId={}", returnRequestId);

        ReturnRequest returnRequest = returnRequestRepository.findByIdWithDetails(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        if (returnRequest.getRequestType() != ReturnRequest.RequestType.RETURN) {
            throw new OrderStateException("Refund retry is only applicable for return requests");
        }

        if (returnRequest.getRefundStatus() != ReturnRequest.RefundStatus.FAILED) {
            throw new OrderStateException("Refund retry is only available for failed refunds");
        }

        // Must be in PROCESSING status (refund failed during initial processing)
        if (returnRequest.getStatus() != ReturnRequest.ReturnStatus.PROCESSING) {
            throw new OrderStateException("Return request must be in processing status to retry refund");
        }

        Order order = returnRequest.getOrder();

        if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
            // COD — mark as processing for manual refund
            returnRequest.setRefundStatus(ReturnRequest.RefundStatus.PROCESSING);
            returnRequest = returnRequestRepository.save(returnRequest);
            log.info("COD refund retry — marked for manual processing: returnRequestId={}", returnRequestId);
            return toDTO(returnRequest);
        }

        String paymentId = order.getPaymentId();
        if (paymentId == null || paymentId.isEmpty()) {
            throw new OrderStateException("No payment ID found for this order. Manual refund required.");
        }

        try {
            RazorpayService.RefundResponse refundResponse = razorpayService.createRefund(
                    paymentId,
                    returnRequest.getRefundAmount(),
                    "Refund retry — Return request: " + returnRequest.getReason()
            );

            returnRequest.setRefundId(refundResponse.getRefundId());
            returnRequest.setRefundStatus(ReturnRequest.RefundStatus.PROCESSING);

            Long refundAmountPaise = refundResponse.getAmount();
            if (refundAmountPaise != null) {
                returnRequest.setRefundAmount(new BigDecimal(refundAmountPaise).divide(new BigDecimal("100")));
            }

            returnRequest = returnRequestRepository.save(returnRequest);
            log.info("Refund retry succeeded: returnRequestId={}, refundId={}", returnRequestId, refundResponse.getRefundId());
            return toDTO(returnRequest);
        } catch (Exception e) {
            log.error("Refund retry failed for return request {}: {}", returnRequestId, e.getMessage(), e);
            throw new OrderStateException("Refund retry failed: " + e.getMessage());
        }
    }

    // ────────────────────── Counts ──────────────────────

    public Long getReturnRequestCount() {
        return returnRequestRepository.count();
    }

    public Long getPendingReturnRequestCount() {
        return returnRequestRepository.countByStatus(ReturnRequest.ReturnStatus.PENDING);
    }

    // ────────────────────── Private Helpers ──────────────────────

    private void validateTransition(ReturnRequest.ReturnStatus from, ReturnRequest.ReturnStatus to) {
        Set<ReturnRequest.ReturnStatus> allowed = VALID_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new OrderStateException(
                    "Invalid status transition: " + from.name() + " → " + to.name());
        }
    }

    private void initiateRefund(ReturnRequest returnRequest, Order order) {
        String paymentId = order.getPaymentId();

        if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
            returnRequest.setRefundStatus(ReturnRequest.RefundStatus.PROCESSING);
            log.info("COD return — manual refund required: returnRequestId={}, orderNumber={}, amount={}",
                    returnRequest.getId(), order.getOrderNumber(), returnRequest.getRefundAmount());
        } else if (paymentId != null && !paymentId.isEmpty()) {
            try {
                RazorpayService.RefundResponse refundResponse = razorpayService.createRefund(
                        paymentId,
                        returnRequest.getRefundAmount(),
                        "Return request: " + returnRequest.getReason()
                );

                returnRequest.setRefundId(refundResponse.getRefundId());
                returnRequest.setRefundStatus(ReturnRequest.RefundStatus.PROCESSING);

                Long refundAmountPaise = refundResponse.getAmount();
                if (refundAmountPaise != null) {
                    returnRequest.setRefundAmount(new BigDecimal(refundAmountPaise).divide(new BigDecimal("100")));
                }

                log.info("Refund initiated: returnRequestId={}, refundId={}",
                        returnRequest.getId(), refundResponse.getRefundId());
            } catch (Exception e) {
                log.error("Error processing refund for return request {}: {}",
                        returnRequest.getId(), e.getMessage(), e);
                returnRequest.setRefundStatus(ReturnRequest.RefundStatus.FAILED);
            }
        } else {
            log.warn("No payment ID found for return refund: returnRequestId={}, orderNumber={}",
                    returnRequest.getId(), order.getOrderNumber());
            returnRequest.setRefundStatus(ReturnRequest.RefundStatus.FAILED);
        }
    }

    private void reserveExchangeStock(ReturnRequest returnRequest) {
        if (returnRequest.getExchangeVariantId() == null) {
            return;
        }

        // Pessimistic lock to prevent race conditions
        ProductVariant exchangeVariant = productVariantRepository.findByIdForUpdate(returnRequest.getExchangeVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Exchange variant not found"));

        int requiredQty = returnRequest.getOrderItem().getQuantity();
        if (exchangeVariant.getStockQuantity() < requiredQty) {
            throw new InsufficientStockException(
                    exchangeVariant.getStockQuantity(), 0,
                    exchangeVariant.getStockQuantity(), requiredQty);
        }

        int quantityBefore = exchangeVariant.getStockQuantity();
        exchangeVariant.setStockQuantity(quantityBefore - requiredQty);
        productVariantRepository.save(exchangeVariant);

        stockMovementService.recordMovement(exchangeVariant,
                returnRequest.getOrderItem().getProduct(),
                com.kalon.entity.StockMovement.MovementType.EXCHANGE_RESERVED,
                -requiredQty, quantityBefore, exchangeVariant.getStockQuantity(),
                com.kalon.entity.StockMovement.ReferenceType.RETURN_REQUEST, returnRequest.getId(),
                "Exchange stock reserved for return request");

        log.info("Exchange stock reserved: exchangeVariantId={}, quantity={}",
                returnRequest.getExchangeVariantId(), requiredQty);
    }

    private void restoreReturnedItemStock(ReturnRequest returnRequest) {
        OrderItem orderItem = returnRequest.getOrderItem();

        // Pessimistic lock to prevent race conditions
        ProductVariant variant = productVariantRepository.findByProductIdAndSizeAndColorForUpdate(
                orderItem.getProduct().getId(),
                orderItem.getSize(),
                orderItem.getColor()
        ).orElse(null);

        if (variant != null) {
            int quantityBefore = variant.getStockQuantity();
            variant.setStockQuantity(quantityBefore + orderItem.getQuantity());
            productVariantRepository.save(variant);

            stockMovementService.recordMovement(variant, orderItem.getProduct(),
                    com.kalon.entity.StockMovement.MovementType.RETURN_COMPLETED,
                    orderItem.getQuantity(), quantityBefore, variant.getStockQuantity(),
                    com.kalon.entity.StockMovement.ReferenceType.RETURN_REQUEST, returnRequest.getId(),
                    "Return completed for order item");

            log.info("Stock restored for completed return: variantId={}, quantity={}",
                    variant.getId(), orderItem.getQuantity());
        } else {
            log.warn("Could not find variant to restore stock for return request {}. Product: {}, Size: {}, Color: {}",
                    returnRequest.getId(),
                    orderItem.getProduct().getId(),
                    orderItem.getSize(),
                    orderItem.getColor());
        }
    }

    private ReturnRequestDTO toDTO(ReturnRequest returnRequest) {
        String exchangeProductName = null;
        String exchangeVariantInfo = null;

        if (returnRequest.getExchangeProductId() != null) {
            exchangeProductName = productRepository.findById(returnRequest.getExchangeProductId())
                    .map(Product::getName).orElse(null);
        }

        if (returnRequest.getExchangeVariantId() != null) {
            exchangeVariantInfo = productVariantRepository.findById(returnRequest.getExchangeVariantId())
                    .map(v -> v.getSize() + " - " + v.getColor())
                    .orElse(null);
        }

        return ReturnRequestDTO.builder()
                .id(returnRequest.getId())
                .orderId(returnRequest.getOrder().getId())
                .orderNumber(returnRequest.getOrder().getOrderNumber())
                .orderItem(toOrderItemDTO(returnRequest.getOrderItem()))
                .requestType(returnRequest.getRequestType().name())
                .status(returnRequest.getStatus().name())
                .reason(returnRequest.getReason())
                .refundAmount(returnRequest.getRefundAmount())
                .refundId(returnRequest.getRefundId())
                .refundStatus(returnRequest.getRefundStatus() != null ? returnRequest.getRefundStatus().name() : null)
                .exchangeProductId(returnRequest.getExchangeProductId())
                .exchangeVariantId(returnRequest.getExchangeVariantId())
                .exchangeProductName(exchangeProductName)
                .exchangeVariantInfo(exchangeVariantInfo)
                .adminNotes(returnRequest.getAdminNotes())
                .createdAt(returnRequest.getCreatedAt())
                .updatedAt(returnRequest.getUpdatedAt())
                .build();
    }

    private OrderItemDTO toOrderItemDTO(OrderItem item) {
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
                .build();
    }
}
