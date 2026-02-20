package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReturnRateDTO {
    private Long productId;
    private String productName;
    private Long totalSold;
    private Long totalReturns;
    private Double returnRatePercent;
}
