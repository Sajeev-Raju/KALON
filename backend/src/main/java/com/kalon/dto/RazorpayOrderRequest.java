package com.kalon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RazorpayOrderRequest {
    @NotNull(message = "Address ID is required")
    private Long addressId;
    private String notes;
}



