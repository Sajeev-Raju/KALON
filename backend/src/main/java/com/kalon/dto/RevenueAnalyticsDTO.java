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
public class RevenueAnalyticsDTO {
    private Map<String, BigDecimal> revenueTimeSeries;
    private Map<String, BigDecimal> revenueByCategory;
    private Map<String, BigDecimal> revenueByPaymentMethod;
    private BigDecimal totalRefunds;
    private Long refundCount;
    private Double refundRatePercent;
    private BigDecimal codRevenue;
    private BigDecimal prepaidRevenue;
    private BigDecimal grossRevenue;
    private BigDecimal netRevenue;
}
