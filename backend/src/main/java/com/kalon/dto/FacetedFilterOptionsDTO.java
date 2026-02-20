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
public class FacetedFilterOptionsDTO {
    private List<FilterCount> sizes;
    private List<FilterCount> colors;
    private List<FilterCount> brands;
    private List<FilterCount> materials;
    private List<FilterCount> categories;
    private List<String> genders;
    private PriceRange priceRange;
    private Long onSaleCount;
    private Long inStockCount;
    private Long totalProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterCount {
        private String value;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRange {
        private BigDecimal min;
        private BigDecimal max;
        private BigDecimal avg;
    }
}
