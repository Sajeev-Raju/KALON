package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesAnalyticsDTO {
    private Map<String, BigDecimal> revenueTimeSeries;
    private Map<String, Long> orderCountTimeSeries;
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private BigDecimal averageOrderValue;
    private Map<String, BigDecimal> revenueByPaymentMethod;
    private BigDecimal previousPeriodRevenue;
    private Double revenueChangePercent;
}
