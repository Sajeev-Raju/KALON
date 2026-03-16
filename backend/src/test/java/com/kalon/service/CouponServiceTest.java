package com.kalon.service;

import com.kalon.dto.CouponDiscountResponse;
import com.kalon.entity.Coupon;
import com.kalon.repository.CouponRepository;
import com.kalon.repository.CouponUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponUsageRepository couponUsageRepository;

    @InjectMocks private CouponService couponService;

    @Test
    void validateAndCalculateDiscount_percentageCoupon() {
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("SAVE20")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20"))
                .minimumOrderAmount(new BigDecimal("500"))
                .maximumDiscountAmount(new BigDecimal("200"))
                .isActive(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .perUserLimit(3)
                .build();

        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(coupon));
        when(couponUsageRepository.countByCouponIdAndUserId(1L, 100L)).thenReturn(0L);

        CouponDiscountResponse response = couponService.validateAndCalculateDiscount(
                "SAVE20", new BigDecimal("1000"), 100L);

        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getCouponCode()).isEqualTo("SAVE20");
    }

    @Test
    void validateAndCalculateDiscount_fixedAmountCoupon() {
        Coupon coupon = Coupon.builder()
                .id(2L)
                .code("FLAT100")
                .discountType(Coupon.DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("100"))
                .isActive(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .build();

        when(couponRepository.findByCodeIgnoreCase("FLAT100")).thenReturn(Optional.of(coupon));

        CouponDiscountResponse response = couponService.validateAndCalculateDiscount(
                "FLAT100", new BigDecimal("500"), 100L);

        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void validateAndCalculateDiscount_expiredCoupon() {
        Coupon coupon = Coupon.builder()
                .id(3L)
                .code("EXPIRED")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10"))
                .isActive(true)
                .validFrom(LocalDateTime.now().minusDays(30))
                .validUntil(LocalDateTime.now().minusDays(1))
                .build();

        when(couponRepository.findByCodeIgnoreCase("EXPIRED")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validateAndCalculateDiscount(
                "EXPIRED", new BigDecimal("1000"), 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validateAndCalculateDiscount_minimumOrderNotMet() {
        Coupon coupon = Coupon.builder()
                .id(4L)
                .code("MIN500")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10"))
                .minimumOrderAmount(new BigDecimal("500"))
                .isActive(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .build();

        when(couponRepository.findByCodeIgnoreCase("MIN500")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validateAndCalculateDiscount(
                "MIN500", new BigDecimal("200"), 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum order amount");
    }

    @Test
    void validateAndCalculateDiscount_invalidCode() {
        when(couponRepository.findByCodeIgnoreCase("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.validateAndCalculateDiscount(
                "INVALID", new BigDecimal("1000"), 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid coupon code");
    }
}
