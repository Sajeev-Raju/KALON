package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercentage;
    private Long categoryId;
    private String categoryName;
    private String brandName;
    private String material;
    private String careInstructions;
    private String countryOfOrigin;
    private boolean isFeatured;
    private boolean isNewArrival;
    private boolean isBestSeller;
    private Double averageRating;
    private Integer reviewCount;
    private boolean isReturnable = true;
    private List<ProductImageDTO> images;
    private List<ProductVariantDTO> variants;
    private List<String> availableSizes;
    private List<String> availableColors;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String ogImage;
}
