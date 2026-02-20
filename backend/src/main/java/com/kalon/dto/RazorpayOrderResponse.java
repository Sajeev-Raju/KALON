package com.kalon.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RazorpayOrderResponse {
    private String razorpayOrderId;
    private Long amount;
    private Long amountInPaise;
    private String currency;
    private String keyId;
    private String orderId;
}



