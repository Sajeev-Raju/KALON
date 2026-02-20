package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertDTO {
    private Long productId;
    private String productName;
    private Long variantId;
    private String size;
    private String color;
    private Integer stockQuantity;
    private Integer reorderPoint;
}
