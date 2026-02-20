package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttemptDTO {
    private Long id;
    private Long orderId;
    private Integer attemptNumber;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String status;
    private String failureReason;
    private String paymentMethod;
    private String source;
    private LocalDateTime createdAt;
}
