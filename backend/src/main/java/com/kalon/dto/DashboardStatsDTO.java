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
public class DashboardStatsDTO {
    private Long totalUsers;
    private Long totalProducts;
    private Long totalOrders;
    private Long totalCategories;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal thisMonthRevenue;
    private Long pendingOrders;
    private Long completedOrders;
    private Long lowStockProducts;
    private Long totalReturnRequests;
    private Long pendingReturnRequests;
    private Map<String, Long> ordersByStatus;
    private Map<String, BigDecimal> revenueByMonth;
}


