package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {
    private Long id;
    private String size;
    private String color;
    private String colorCode;
    private String sku;
    private Integer stockQuantity;
    private boolean isAvailable;
    private Integer reorderPoint;
}
