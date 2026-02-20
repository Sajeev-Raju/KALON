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
public class FilterOptionsDTO {
    private List<String> sizes;
    private List<String> colors;
    private List<String> brands;
    private List<String> materials;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> genders;
}
