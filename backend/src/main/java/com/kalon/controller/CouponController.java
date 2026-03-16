package com.kalon.controller;

import com.kalon.dto.ApiResponse;
import com.kalon.dto.ApplyCouponRequest;
import com.kalon.dto.CouponDiscountResponse;
import com.kalon.entity.Cart;
import com.kalon.entity.CartItem;
import com.kalon.entity.User;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.CartRepository;
import com.kalon.repository.UserRepository;
import com.kalon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponDiscountResponse>> validateCoupon(
            @Valid @RequestBody ApplyCouponRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            subtotal = subtotal.add(item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        CouponDiscountResponse response = couponService.validateAndCalculateDiscount(
                request.getCode(), subtotal, user.getId());

        return ResponseEntity.ok(ApiResponse.success("Coupon validated", response));
    }
}
