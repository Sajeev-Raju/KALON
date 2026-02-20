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
public class OverviewAnalyticsDTO {
    private BigDecimal todayRevenue;
    private BigDecimal yesterdayRevenue;
    private Double todayRevenueChangePercent;

    private Long todayOrders;
    private Long yesterdayOrders;
    private Double todayOrdersChangePercent;

    private Long todayNewUsers;
    private Long yesterdayNewUsers;
    private Double todayNewUsersChangePercent;

    private BigDecimal thisWeekRevenue;
    private BigDecimal lastWeekRevenue;
    private Double weekRevenueChangePercent;

    private BigDecimal thisMonthRevenue;
    private BigDecimal lastMonthRevenue;
    private Double monthRevenueChangePercent;

    private Double ordersPerActiveUser;
    private Double averageItemsPerOrder;
    private Double orderFulfillmentRatePercent;
    private Double returnRatePercent;

    private Long pendingOrders;
    private Long pendingReturns;
    private Long lowStockCount;
    private Long unapprovedReviews;
}
