package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnalyticsDTO {
    private List<TopProductDTO> topSellingByRevenue;
    private List<TopProductDTO> topSellingByQuantity;
    private List<CategoryPerformanceDTO> categoryPerformance;
    private List<LowStockAlertDTO> lowStockAlerts;
    private List<ProductReturnRateDTO> highReturnRateProducts;
}
