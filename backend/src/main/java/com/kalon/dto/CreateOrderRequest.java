package com.kalon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotNull(message = "Shipping address ID is required")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    private String notes;

    private String couponCode;
}
