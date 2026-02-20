package com.kalon.controller;

import com.kalon.dto.ApiResponse;
import com.kalon.dto.CreateOrderRequest;
import com.kalon.dto.OrderDTO;
import com.kalon.entity.User;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.UserRepository;
import com.kalon.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final com.kalon.service.PaymentMethodConfigService paymentMethodConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrders(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByUserId(userId)));
    }

    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getOrdersPaginated(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrdersByUserId(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(orderId, userId)));
    }

    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDTO>> trackOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String orderNumber) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByNumber(orderNumber, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", orderService.createOrder(userId, request)));
    }

    @PostMapping("/razorpay/create")
    public ResponseEntity<ApiResponse<com.kalon.dto.RazorpayOrderResponse>> createRazorpayOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.kalon.dto.RazorpayOrderRequest request) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Razorpay order created", orderService.createRazorpayOrder(userId, request)));
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<ApiResponse<OrderDTO>> verifyPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.kalon.dto.VerifyPaymentRequest request) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Payment verified and order placed", orderService.verifyAndCompletePayment(userId, request)));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", orderService.cancelOrder(orderId, userId)));
    }

    @PostMapping("/{orderId}/retry-payment")
    public ResponseEntity<ApiResponse<com.kalon.dto.RazorpayOrderResponse>> retryPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Payment retry initiated", orderService.retryPayment(userId, orderId)));
    }

    @GetMapping("/{orderId}/payment-attempts")
    public ResponseEntity<ApiResponse<java.util.List<com.kalon.dto.PaymentAttemptDTO>>> getPaymentAttempts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(orderService.getPaymentAttempts(orderId, userId)));
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<java.util.List<com.kalon.dto.PaymentMethodConfigDTO>>> getEnabledPaymentMethods() {
        return ResponseEntity.ok(ApiResponse.success(paymentMethodConfigService.getEnabledMethods()));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}
