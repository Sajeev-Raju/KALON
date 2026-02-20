package com.kalon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodConfigDTO {
    private Long id;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String description;

    @NotNull(message = "Enabled status is required")
    private Boolean isEnabled;

    private Integer displayOrder;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
}
