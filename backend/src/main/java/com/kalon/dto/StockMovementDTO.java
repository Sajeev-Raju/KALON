package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDTO {
    private Long id;
    private Long variantId;
    private Long productId;
    private String productName;
    private String variantSize;
    private String variantColor;
    private String movementType;
    private Integer quantityChange;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String referenceType;
    private Long referenceId;
    private String notes;
    private String createdByEmail;
    private LocalDateTime createdAt;
}
