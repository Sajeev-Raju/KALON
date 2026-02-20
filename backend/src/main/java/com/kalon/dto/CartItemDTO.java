package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Long variantId;
    private String size;
    private String color;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private Integer stockQuantity;
    private Boolean isAvailable;
}
