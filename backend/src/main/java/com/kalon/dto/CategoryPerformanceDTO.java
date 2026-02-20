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
public class CategoryPerformanceDTO {
    private Long categoryId;
    private String categoryName;
    private Long totalProducts;
    private Long unitsSold;
    private BigDecimal revenue;
}
