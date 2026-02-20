package com.kalon.service;

import com.kalon.dto.*;
import com.kalon.entity.Order;
import com.kalon.entity.ReturnRequest;
import com.kalon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final SiteConfigService siteConfigService;

    public DashboardStatsDTO getDashboardStats() {
        Long totalUsers = userRepository.count();
        Long totalProducts = productRepository.count();
        Long totalOrders = orderRepository.count();
        Long totalCategories = categoryRepository.count();

        // Calculate revenue using aggregation queries (no loading all orders into memory)
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByStatus(Order.OrderStatus.DELIVERED);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        BigDecimal todayRevenue = orderRepository.sumTotalAmountByStatusAndCreatedAtAfter(
                Order.OrderStatus.DELIVERED, startOfToday);

        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        BigDecimal thisMonthRevenue = orderRepository.sumTotalAmountByStatusAndCreatedAtAfter(
                Order.OrderStatus.DELIVERED, startOfMonth);

        Long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.PENDING);
        Long completedOrders = orderRepository.countByStatus(Order.OrderStatus.DELIVERED);

        // Count low stock products using aggregation query
        Long lowStockProducts = productVariantRepository.countLowStock(siteConfigService.getLowStockThreshold());

        Long totalReturnRequests = returnRequestRepository.count();
        Long pendingReturnRequests = returnRequestRepository.countByStatus(
                com.kalon.entity.ReturnRequest.ReturnStatus.PENDING);

        // Orders by status using aggregation query
        Map<String, Long> ordersByStatus = new HashMap<>();
        List<Object[]> statusCounts = orderRepository.countGroupByStatus();
        for (Object[] row : statusCounts) {
            Order.OrderStatus status = (Order.OrderStatus) row[0];
            Long count = (Long) row[1];
            ordersByStatus.put(status.name(), count);
        }

        // Revenue by month (last 6 months) using aggregation queries
        Map<String, BigDecimal> revenueByMonth = new java.util.LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1);
            LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
            LocalDateTime monthEndDateTime = monthEnd.atStartOfDay();

            BigDecimal monthRevenue = orderRepository.sumTotalAmountByStatusAndCreatedAtBetween(
                    Order.OrderStatus.DELIVERED, monthStartDateTime, monthEndDateTime);

            revenueByMonth.put(monthStart.getMonth().name() + " " + monthStart.getYear(), monthRevenue);
        }

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalCategories(totalCategories)
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .lowStockProducts(lowStockProducts)
                .totalReturnRequests(totalReturnRequests)
                .pendingReturnRequests(pendingReturnRequests)
                .ordersByStatus(ordersByStatus)
                .revenueByMonth(revenueByMonth)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  1. Sales Analytics
    // ═══════════════════════════════════════════════════════════

    public SalesAnalyticsDTO getSalesAnalytics(String period, LocalDate dateFrom, LocalDate dateTo) {
        if (dateTo == null) dateTo = LocalDate.now().plusDays(1);
        if (dateFrom == null) dateFrom = dateTo.minusDays(30);

        LocalDateTime startDate = dateFrom.atStartOfDay();
        LocalDateTime endDate = dateTo.atStartOfDay();

        // Revenue time-series (daily from DB)
        Map<String, BigDecimal> dailyRevenue = buildDailyRevenueSeries(startDate, endDate, dateFrom, dateTo);
        Map<String, Long> dailyOrderCount = buildDailyOrderCountSeries(startDate, endDate, dateFrom, dateTo);

        // Aggregate if needed
        Map<String, BigDecimal> revenueTimeSeries = dailyRevenue;
        Map<String, Long> orderCountTimeSeries = dailyOrderCount;
        if ("WEEKLY".equalsIgnoreCase(period)) {
            revenueTimeSeries = aggregateByWeek(dailyRevenue);
            orderCountTimeSeries = aggregateCountByWeek(dailyOrderCount);
        } else if ("MONTHLY".equalsIgnoreCase(period)) {
            revenueTimeSeries = aggregateByMonth(dailyRevenue);
            orderCountTimeSeries = aggregateCountByMonth(dailyOrderCount);
        }

        // Totals
        BigDecimal totalRevenue = dailyRevenue.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long totalOrders = dailyOrderCount.values().stream()
                .reduce(0L, Long::sum);
        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Revenue by payment method
        Map<String, BigDecimal> revenueByPaymentMethod = new LinkedHashMap<>();
        for (Object[] row : orderRepository.revenueByPaymentMethodInRange(startDate, endDate)) {
            String method = row[0] != null ? row[0].toString() : "UNKNOWN";
            BigDecimal amount = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            revenueByPaymentMethod.put(method, amount);
        }

        // Previous period comparison
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dateFrom, dateTo);
        LocalDate prevFrom = dateFrom.minusDays(daysBetween);
        LocalDateTime prevStart = prevFrom.atStartOfDay();
        BigDecimal previousPeriodRevenue = BigDecimal.ZERO;
        for (Object[] row : orderRepository.revenueByDateRange(prevStart, startDate)) {
            BigDecimal amt = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            previousPeriodRevenue = previousPeriodRevenue.add(amt);
        }

        return SalesAnalyticsDTO.builder()
                .revenueTimeSeries(revenueTimeSeries)
                .orderCountTimeSeries(orderCountTimeSeries)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .revenueByPaymentMethod(revenueByPaymentMethod)
                .previousPeriodRevenue(previousPeriodRevenue)
                .revenueChangePercent(calcPercentChange(previousPeriodRevenue, totalRevenue))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  2. Product Analytics
    // ═══════════════════════════════════════════════════════════

    public ProductAnalyticsDTO getProductAnalytics(int page, int size, String sortBy,
                                                    LocalDate dateFrom, LocalDate dateTo) {
        if (dateTo == null) dateTo = LocalDate.now().plusDays(1);
        if (dateFrom == null) dateFrom = dateTo.minusDays(30);

        LocalDateTime startDate = dateFrom.atStartOfDay();
        LocalDateTime endDate = dateTo.atStartOfDay();
        Pageable pageable = PageRequest.of(page, size);

        // Top products by revenue
        List<TopProductDTO> topByRevenue = mapToTopProductDTOList(
                orderItemRepository.topProductsByRevenueInRange(startDate, endDate, pageable));

        // Top products by quantity
        List<TopProductDTO> topByQuantity = mapToTopProductDTOList(
                orderItemRepository.topProductsByQuantityInRange(startDate, endDate, pageable));

        // Category performance
        List<CategoryPerformanceDTO> categoryPerformance = new ArrayList<>();
        for (Object[] row : orderItemRepository.categoryPerformanceInRange(startDate, endDate)) {
            categoryPerformance.add(CategoryPerformanceDTO.builder()
                    .categoryId(((Number) row[0]).longValue())
                    .categoryName((String) row[1])
                    .totalProducts(((Number) row[2]).longValue())
                    .unitsSold(((Number) row[3]).longValue())
                    .revenue(new BigDecimal(row[4].toString()))
                    .build());
        }

        // Low stock alerts (configurable threshold, top 20)
        int threshold = siteConfigService.getLowStockThreshold();
        List<LowStockAlertDTO> lowStockAlerts = new ArrayList<>();
        for (Object[] row : productVariantRepository.findLowStockVariantsWithDetails(threshold, PageRequest.of(0, 20))) {
            lowStockAlerts.add(LowStockAlertDTO.builder()
                    .variantId(((Number) row[0]).longValue())
                    .productId(((Number) row[1]).longValue())
                    .productName((String) row[2])
                    .size((String) row[3])
                    .color((String) row[4])
                    .stockQuantity(((Number) row[5]).intValue())
                    .reorderPoint(row[6] != null ? ((Number) row[6]).intValue() : null)
                    .build());
        }

        // Return rate by product (top 10 returned products)
        List<ProductReturnRateDTO> highReturnRateProducts = new ArrayList<>();
        for (Object[] row : returnRequestRepository.returnCountByProduct(PageRequest.of(0, 10))) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            Long totalReturns = ((Number) row[2]).longValue();
            Long totalSold = orderItemRepository.totalUnitsSoldForProduct(productId);
            double returnRate = totalSold > 0 ? (totalReturns * 100.0) / totalSold : 0.0;
            highReturnRateProducts.add(ProductReturnRateDTO.builder()
                    .productId(productId)
                    .productName(productName)
                    .totalSold(totalSold)
                    .totalReturns(totalReturns)
                    .returnRatePercent(Math.round(returnRate * 100.0) / 100.0)
                    .build());
        }

        return ProductAnalyticsDTO.builder()
                .topSellingByRevenue(topByRevenue)
                .topSellingByQuantity(topByQuantity)
                .categoryPerformance(categoryPerformance)
                .lowStockAlerts(lowStockAlerts)
                .highReturnRateProducts(highReturnRateProducts)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  3. Customer Analytics
    // ═══════════════════════════════════════════════════════════

    public CustomerAnalyticsDTO getCustomerAnalytics(LocalDate dateFrom, LocalDate dateTo,
                                                      int page, int size) {
        if (dateTo == null) dateTo = LocalDate.now().plusDays(1);
        if (dateFrom == null) dateFrom = dateTo.minusDays(30);

        LocalDateTime startDate = dateFrom.atStartOfDay();
        LocalDateTime endDate = dateTo.atStartOfDay();

        // New vs returning customers
        Long newCustomers = userRepository.countNewOrderingCustomersInRange(startDate, endDate);
        Long returningCustomers = userRepository.countReturningCustomersInRange(startDate, endDate);

        // Registration time-series
        Map<String, Long> registrationTimeSeries = buildDailyCountSeries(
                userRepository.registrationsByDateRange(startDate, endDate), dateFrom, dateTo);

        // Top customers by spend
        List<TopCustomerDTO> topCustomersBySpend = new ArrayList<>();
        for (Object[] row : orderRepository.topCustomersBySpendInRange(startDate, endDate,
                PageRequest.of(page, size))) {
            topCustomersBySpend.add(TopCustomerDTO.builder()
                    .userId(((Number) row[0]).longValue())
                    .firstName((String) row[1])
                    .lastName((String) row[2])
                    .email((String) row[3])
                    .orderCount(((Number) row[4]).longValue())
                    .totalSpent(new BigDecimal(row[5].toString()))
                    .build());
        }

        // Acquisition by provider
        Map<String, Long> acquisitionByProvider = new LinkedHashMap<>();
        for (Object[] row : userRepository.countByProvider()) {
            String provider = row[0] != null ? row[0].toString() : "unknown";
            acquisitionByProvider.put(provider, ((Number) row[1]).longValue());
        }

        // Average orders per customer
        Long distinctCustomers = orderRepository.countDistinctCustomersInRange(startDate, endDate);
        Long totalOrders = orderRepository.countOrdersInRange(startDate, endDate);
        double avgOrdersPerCustomer = distinctCustomers != null && distinctCustomers > 0
                ? Math.round((totalOrders * 100.0) / distinctCustomers) / 100.0 : 0.0;

        // Lifetime value distribution
        Map<String, Long> lifetimeValueDistribution = new LinkedHashMap<>();
        lifetimeValueDistribution.put("0", 0L);
        lifetimeValueDistribution.put("1-500", 0L);
        lifetimeValueDistribution.put("501-2000", 0L);
        lifetimeValueDistribution.put("2001-5000", 0L);
        lifetimeValueDistribution.put("5001-10000", 0L);
        lifetimeValueDistribution.put("10001+", 0L);
        for (Object[] row : userRepository.customerLifetimeValues()) {
            BigDecimal spent = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            String bucket = categorizeLTV(spent);
            lifetimeValueDistribution.merge(bucket, 1L, Long::sum);
        }

        return CustomerAnalyticsDTO.builder()
                .newCustomers(newCustomers != null ? newCustomers : 0L)
                .returningCustomers(returningCustomers != null ? returningCustomers : 0L)
                .registrationTimeSeries(registrationTimeSeries)
                .topCustomersBySpend(topCustomersBySpend)
                .acquisitionByProvider(acquisitionByProvider)
                .averageOrdersPerCustomer(avgOrdersPerCustomer)
                .lifetimeValueDistribution(lifetimeValueDistribution)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  4. Revenue Analytics
    // ═══════════════════════════════════════════════════════════

    public RevenueAnalyticsDTO getRevenueAnalytics(LocalDate dateFrom, LocalDate dateTo, String groupBy) {
        if (dateTo == null) dateTo = LocalDate.now().plusDays(1);
        if (dateFrom == null) dateFrom = dateTo.minusDays(30);

        LocalDateTime startDate = dateFrom.atStartOfDay();
        LocalDateTime endDate = dateTo.atStartOfDay();

        // Revenue time-series
        Map<String, BigDecimal> dailyRevenue = buildDailyRevenueSeries(startDate, endDate, dateFrom, dateTo);
        Map<String, BigDecimal> revenueTimeSeries = dailyRevenue;
        if ("WEEK".equalsIgnoreCase(groupBy)) {
            revenueTimeSeries = aggregateByWeek(dailyRevenue);
        } else if ("MONTH".equalsIgnoreCase(groupBy)) {
            revenueTimeSeries = aggregateByMonth(dailyRevenue);
        }

        // Revenue by category
        Map<String, BigDecimal> revenueByCategory = new LinkedHashMap<>();
        for (Object[] row : orderRepository.revenueByCategoryInRange(startDate, endDate)) {
            revenueByCategory.put((String) row[1], new BigDecimal(row[2].toString()));
        }

        // Revenue by payment method
        Map<String, BigDecimal> revenueByPaymentMethod = new LinkedHashMap<>();
        for (Object[] row : orderRepository.revenueByPaymentMethodInRange(startDate, endDate)) {
            String method = row[0] != null ? row[0].toString() : "UNKNOWN";
            revenueByPaymentMethod.put(method, new BigDecimal(row[1].toString()));
        }

        // Refund summary
        BigDecimal totalRefunds = returnRequestRepository.totalRefundsInRange(startDate, endDate);
        if (totalRefunds == null) totalRefunds = BigDecimal.ZERO;
        Long refundCount = returnRequestRepository.countCompletedReturnsInRange(startDate, endDate);
        if (refundCount == null) refundCount = 0L;

        Long deliveredOrders = orderRepository.countOrdersInRange(startDate, endDate);
        double refundRatePercent = deliveredOrders != null && deliveredOrders > 0
                ? Math.round((refundCount * 10000.0) / deliveredOrders) / 100.0 : 0.0;

        // COD vs Prepaid split
        BigDecimal codRevenue = revenueByPaymentMethod.getOrDefault("COD", BigDecimal.ZERO);
        BigDecimal prepaidRevenue = revenueByPaymentMethod.entrySet().stream()
                .filter(e -> !"COD".equals(e.getKey()) && !"UNKNOWN".equals(e.getKey()))
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Gross and net revenue
        BigDecimal grossRevenue = dailyRevenue.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netRevenue = grossRevenue.subtract(totalRefunds);

        return RevenueAnalyticsDTO.builder()
                .revenueTimeSeries(revenueTimeSeries)
                .revenueByCategory(revenueByCategory)
                .revenueByPaymentMethod(revenueByPaymentMethod)
                .totalRefunds(totalRefunds)
                .refundCount(refundCount)
                .refundRatePercent(refundRatePercent)
                .codRevenue(codRevenue)
                .prepaidRevenue(prepaidRevenue)
                .grossRevenue(grossRevenue)
                .netRevenue(netRevenue)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  5. Overview Analytics (KPI Dashboard)
    // ═══════════════════════════════════════════════════════════

    public OverviewAnalyticsDTO getOverviewAnalytics() {
        LocalDate today = LocalDate.now();

        // Today vs yesterday
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        BigDecimal todayRevenue = sumRevenue(orderRepository.revenueByDateRange(todayStart, tomorrowStart));
        BigDecimal yesterdayRevenue = sumRevenue(orderRepository.revenueByDateRange(yesterdayStart, todayStart));

        Long todayOrders = orderRepository.countOrdersInRange(todayStart, tomorrowStart);
        Long yesterdayOrders = orderRepository.countOrdersInRange(yesterdayStart, todayStart);

        Long todayNewUsers = userRepository.countNewUsersInRange(todayStart, tomorrowStart);
        Long yesterdayNewUsers = userRepository.countNewUsersInRange(yesterdayStart, todayStart);

        // This week vs last week
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate lastWeekStart = weekStart.minusWeeks(1);
        BigDecimal thisWeekRevenue = sumRevenue(orderRepository.revenueByDateRange(
                weekStart.atStartOfDay(), tomorrowStart));
        BigDecimal lastWeekRevenue = sumRevenue(orderRepository.revenueByDateRange(
                lastWeekStart.atStartOfDay(), weekStart.atStartOfDay()));

        // This month vs last month
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        BigDecimal thisMonthRevenue = sumRevenue(orderRepository.revenueByDateRange(
                monthStart.atStartOfDay(), tomorrowStart));
        BigDecimal lastMonthRevenue = sumRevenue(orderRepository.revenueByDateRange(
                lastMonthStart.atStartOfDay(), monthStart.atStartOfDay()));

        // Orders per active user
        Long activeUsers = userRepository.countActiveUsers();
        Long totalNonCancelled = orderRepository.countOrdersInRange(
                LocalDateTime.of(2000, 1, 1, 0, 0), tomorrowStart);
        double ordersPerActiveUser = activeUsers != null && activeUsers > 0
                ? Math.round((totalNonCancelled * 100.0) / activeUsers) / 100.0 : 0.0;

        // Average items per order
        Double avgItems = orderRepository.averageItemsPerOrder();
        double averageItemsPerOrder = avgItems != null
                ? Math.round(avgItems * 100.0) / 100.0 : 0.0;

        // Fulfillment rate: delivered / (total - cancelled) in last 30 days
        LocalDateTime last30Start = today.minusDays(30).atStartOfDay();
        List<Object[]> statusCounts = orderRepository.countGroupByStatusInRange(last30Start, tomorrowStart);
        long totalInPeriod = 0;
        long deliveredInPeriod = 0;
        long cancelledInPeriod = 0;
        for (Object[] row : statusCounts) {
            Order.OrderStatus status = (Order.OrderStatus) row[0];
            long count = ((Number) row[1]).longValue();
            totalInPeriod += count;
            if (status == Order.OrderStatus.DELIVERED) deliveredInPeriod = count;
            if (status == Order.OrderStatus.CANCELLED) cancelledInPeriod = count;
        }
        long fulfillmentBase = totalInPeriod - cancelledInPeriod;
        double fulfillmentRate = fulfillmentBase > 0
                ? Math.round((deliveredInPeriod * 10000.0) / fulfillmentBase) / 100.0 : 0.0;

        // Return rate: completed returns / delivered orders (all time)
        Long completedReturns = returnRequestRepository.countAllCompletedReturns();
        Long deliveredOrders = orderRepository.countByStatus(Order.OrderStatus.DELIVERED);
        double returnRate = deliveredOrders != null && deliveredOrders > 0 && completedReturns != null
                ? Math.round((completedReturns * 10000.0) / deliveredOrders) / 100.0 : 0.0;

        // Pending actions
        Long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.PENDING);
        Long pendingReturns = returnRequestRepository.countByStatus(ReturnRequest.ReturnStatus.PENDING);
        Long lowStockCount = productVariantRepository.countLowStock(siteConfigService.getLowStockThreshold());
        Long unapprovedReviews = reviewRepository.countUnapprovedReviews();

        return OverviewAnalyticsDTO.builder()
                .todayRevenue(todayRevenue)
                .yesterdayRevenue(yesterdayRevenue)
                .todayRevenueChangePercent(calcPercentChange(yesterdayRevenue, todayRevenue))
                .todayOrders(todayOrders != null ? todayOrders : 0L)
                .yesterdayOrders(yesterdayOrders != null ? yesterdayOrders : 0L)
                .todayOrdersChangePercent(calcPercentChange(
                        yesterdayOrders != null ? yesterdayOrders : 0L,
                        todayOrders != null ? todayOrders : 0L))
                .todayNewUsers(todayNewUsers != null ? todayNewUsers : 0L)
                .yesterdayNewUsers(yesterdayNewUsers != null ? yesterdayNewUsers : 0L)
                .todayNewUsersChangePercent(calcPercentChange(
                        yesterdayNewUsers != null ? yesterdayNewUsers : 0L,
                        todayNewUsers != null ? todayNewUsers : 0L))
                .thisWeekRevenue(thisWeekRevenue)
                .lastWeekRevenue(lastWeekRevenue)
                .weekRevenueChangePercent(calcPercentChange(lastWeekRevenue, thisWeekRevenue))
                .thisMonthRevenue(thisMonthRevenue)
                .lastMonthRevenue(lastMonthRevenue)
                .monthRevenueChangePercent(calcPercentChange(lastMonthRevenue, thisMonthRevenue))
                .ordersPerActiveUser(ordersPerActiveUser)
                .averageItemsPerOrder(averageItemsPerOrder)
                .orderFulfillmentRatePercent(fulfillmentRate)
                .returnRatePercent(returnRate)
                .pendingOrders(pendingOrders != null ? pendingOrders : 0L)
                .pendingReturns(pendingReturns != null ? pendingReturns : 0L)
                .lowStockCount(lowStockCount != null ? lowStockCount : 0L)
                .unapprovedReviews(unapprovedReviews != null ? unapprovedReviews : 0L)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  Private helpers
    // ═══════════════════════════════════════════════════════════

    private Map<String, BigDecimal> buildDailyRevenueSeries(LocalDateTime startDate, LocalDateTime endDate,
                                                             LocalDate dateFrom, LocalDate dateTo) {
        Map<String, BigDecimal> series = new LinkedHashMap<>();
        // Pre-fill all dates with zero
        for (LocalDate d = dateFrom; d.isBefore(dateTo); d = d.plusDays(1)) {
            series.put(d.toString(), BigDecimal.ZERO);
        }
        // Overlay DB results
        for (Object[] row : orderRepository.revenueByDateRange(startDate, endDate)) {
            String date = row[0].toString();
            BigDecimal amount = new BigDecimal(row[1].toString());
            series.put(date, amount);
        }
        return series;
    }

    private Map<String, Long> buildDailyOrderCountSeries(LocalDateTime startDate, LocalDateTime endDate,
                                                           LocalDate dateFrom, LocalDate dateTo) {
        Map<String, Long> series = new LinkedHashMap<>();
        for (LocalDate d = dateFrom; d.isBefore(dateTo); d = d.plusDays(1)) {
            series.put(d.toString(), 0L);
        }
        for (Object[] row : orderRepository.orderCountByDateRange(startDate, endDate)) {
            String date = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            series.put(date, count);
        }
        return series;
    }

    private Map<String, Long> buildDailyCountSeries(List<Object[]> rows,
                                                      LocalDate dateFrom, LocalDate dateTo) {
        Map<String, Long> series = new LinkedHashMap<>();
        for (LocalDate d = dateFrom; d.isBefore(dateTo); d = d.plusDays(1)) {
            series.put(d.toString(), 0L);
        }
        for (Object[] row : rows) {
            String date = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            series.put(date, count);
        }
        return series;
    }

    private Map<String, BigDecimal> aggregateByWeek(Map<String, BigDecimal> dailyData) {
        Map<String, BigDecimal> weekly = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : dailyData.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey());
            int weekNum = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int year = date.get(IsoFields.WEEK_BASED_YEAR);
            String key = year + "-W" + String.format("%02d", weekNum);
            weekly.merge(key, entry.getValue(), BigDecimal::add);
        }
        return weekly;
    }

    private Map<String, BigDecimal> aggregateByMonth(Map<String, BigDecimal> dailyData) {
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : dailyData.entrySet()) {
            String key = entry.getKey().substring(0, 7); // "2026-01"
            monthly.merge(key, entry.getValue(), BigDecimal::add);
        }
        return monthly;
    }

    private Map<String, Long> aggregateCountByWeek(Map<String, Long> dailyData) {
        Map<String, Long> weekly = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : dailyData.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey());
            int weekNum = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int year = date.get(IsoFields.WEEK_BASED_YEAR);
            String key = year + "-W" + String.format("%02d", weekNum);
            weekly.merge(key, entry.getValue(), Long::sum);
        }
        return weekly;
    }

    private Map<String, Long> aggregateCountByMonth(Map<String, Long> dailyData) {
        Map<String, Long> monthly = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : dailyData.entrySet()) {
            String key = entry.getKey().substring(0, 7);
            monthly.merge(key, entry.getValue(), Long::sum);
        }
        return monthly;
    }

    private Double calcPercentChange(BigDecimal previous, BigDecimal current) {
        if (previous == null) previous = BigDecimal.ZERO;
        if (current == null) current = BigDecimal.ZERO;
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? 0.0 : 100.0;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Double calcPercentChange(Long previous, Long current) {
        if (previous == null) previous = 0L;
        if (current == null) current = 0L;
        if (previous == 0) {
            return current == 0 ? 0.0 : 100.0;
        }
        return Math.round(((current - previous) * 10000.0) / previous) / 100.0;
    }

    private BigDecimal sumRevenue(List<Object[]> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : rows) {
            if (row[1] != null) {
                total = total.add(new BigDecimal(row[1].toString()));
            }
        }
        return total;
    }

    private List<TopProductDTO> mapToTopProductDTOList(List<Object[]> rows) {
        List<TopProductDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(TopProductDTO.builder()
                    .productId(((Number) row[0]).longValue())
                    .productName((String) row[1])
                    .categoryName((String) row[2])
                    .unitsSold(((Number) row[3]).longValue())
                    .revenue(new BigDecimal(row[4].toString()))
                    .averageRating(((Number) row[5]).doubleValue())
                    .reviewCount(((Number) row[6]).intValue())
                    .build());
        }
        return result;
    }

    private String categorizeLTV(BigDecimal spent) {
        if (spent.compareTo(BigDecimal.ZERO) == 0) return "0";
        if (spent.compareTo(BigDecimal.valueOf(500)) <= 0) return "1-500";
        if (spent.compareTo(BigDecimal.valueOf(2000)) <= 0) return "501-2000";
        if (spent.compareTo(BigDecimal.valueOf(5000)) <= 0) return "2001-5000";
        if (spent.compareTo(BigDecimal.valueOf(10000)) <= 0) return "5001-10000";
        return "10001+";
    }
}


