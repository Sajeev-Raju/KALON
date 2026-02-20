package com.kalon.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductSearchRequest {
    private String keyword;
    private Long categoryId;
    private String categorySlug;
    private String gender;
    private List<String> sizes;
    private List<String> colors;
    private List<String> brands;
    private List<String> materials;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStock;
    private Boolean onSale;
    private Boolean featured;
    private Boolean newArrival;
    private Boolean bestSeller;
    private String sortBy;
    private String sortDir;
    private Integer page;
    private Integer size;
}
