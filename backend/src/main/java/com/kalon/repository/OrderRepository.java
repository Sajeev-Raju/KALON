package com.kalon.repository;

import com.kalon.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.images"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.images"})
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.images"})
    @Query("SELECT o FROM Order o WHERE o.razorpayOrderId = :razorpayOrderId")
    Optional<Order> findByRazorpayOrderIdWithItems(@Param("razorpayOrderId") String razorpayOrderId);

    List<Order> findByStatus(Order.OrderStatus status);
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
    Optional<Order> findByPaymentId(String paymentId);
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.paymentStatus = :paymentStatus AND o.createdAt < :expiry")
    List<Order> findExpiredPendingOrders(
            @Param("status") Order.OrderStatus status,
            @Param("paymentStatus") Order.PaymentStatus paymentStatus,
            @Param("expiry") java.time.LocalDateTime expiry);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items oi WHERE o.user.id = :userId AND oi.product.id = :productId AND o.status = :status")
    boolean existsByUserIdAndProductIdAndStatus(@Param("userId") Long userId, @Param("productId") Long productId, @Param("status") Order.OrderStatus status);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items oi WHERE o.user.id = :userId AND oi.product.id = :productId AND o.status IN :statuses")
    boolean existsByUserIdAndProductIdAndStatusIn(@Param("userId") Long userId, @Param("productId") Long productId, @Param("statuses") java.util.Collection<Order.OrderStatus> statuses);

    default boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return existsByUserIdAndProductIdAndStatusIn(userId, productId,
                java.util.List.of(Order.OrderStatus.DELIVERED, Order.OrderStatus.RETURNED));
    }

    // Aggregation queries for dashboard performance
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    java.math.BigDecimal sumTotalAmountByStatus(@Param("status") Order.OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.createdAt >= :startDate")
    java.math.BigDecimal sumTotalAmountByStatusAndCreatedAtAfter(
            @Param("status") Order.OrderStatus status,
            @Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    java.math.BigDecimal sumTotalAmountByStatusAndCreatedAtBetween(
            @Param("status") Order.OrderStatus status,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    Long countByStatus(Order.OrderStatus status);

    // Aggregation queries for UserService performance (avoid N+1)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.id = :userId AND o.status = :status")
    java.math.BigDecimal sumTotalAmountByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Order.OrderStatus status);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countGroupByStatus();

    // Advanced filter queries for admin
    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:dateFrom IS NULL OR o.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR o.createdAt <= :dateTo) AND " +
           "(:minAmount IS NULL OR o.totalAmount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR o.totalAmount <= :maxAmount)")
    Page<Order> findWithFilters(
            @Param("status") Order.OrderStatus status,
            @Param("dateFrom") java.time.LocalDateTime dateFrom,
            @Param("dateTo") java.time.LocalDateTime dateTo,
            @Param("minAmount") java.math.BigDecimal minAmount,
            @Param("maxAmount") java.math.BigDecimal maxAmount,
            Pageable pageable);

    // ─── Analytics queries ───

    @Query(value = "SELECT CAST(o.created_at AS DATE) AS order_date, COALESCE(SUM(o.total_amount), 0) " +
           "FROM orders o WHERE o.status = 'DELIVERED' " +
           "AND o.created_at >= :startDate AND o.created_at < :endDate " +
           "GROUP BY CAST(o.created_at AS DATE) ORDER BY 1", nativeQuery = true)
    List<Object[]> revenueByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT CAST(o.created_at AS DATE) AS order_date, COUNT(*) " +
           "FROM orders o WHERE o.status != 'CANCELLED' " +
           "AND o.created_at >= :startDate AND o.created_at < :endDate " +
           "GROUP BY CAST(o.created_at AS DATE) ORDER BY 1", nativeQuery = true)
    List<Object[]> orderCountByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT o.payment_method, COALESCE(SUM(o.total_amount), 0) " +
           "FROM orders o WHERE o.status = 'DELIVERED' " +
           "AND o.created_at >= :startDate AND o.created_at < :endDate " +
           "GROUP BY o.payment_method", nativeQuery = true)
    List<Object[]> revenueByPaymentMethodInRange(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM orders o WHERE o.status != 'CANCELLED' " +
           "AND o.created_at >= :startDate AND o.created_at < :endDate", nativeQuery = true)
    Long countOrdersInRange(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT c.id, c.name, COALESCE(SUM(oi.total_price), 0) " +
           "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
           "JOIN products p ON oi.product_id = p.id JOIN categories c ON p.category_id = c.id " +
           "WHERE o.status = 'DELIVERED' AND o.created_at >= :startDate AND o.created_at < :endDate " +
           "GROUP BY c.id, c.name ORDER BY 3 DESC", nativeQuery = true)
    List<Object[]> revenueByCategoryInRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o.status, COUNT(o) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate GROUP BY o.status")
    List<Object[]> countGroupByStatusInRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT u.id, u.first_name, u.last_name, u.email, " +
           "COUNT(o.id), COALESCE(SUM(o.total_amount), 0) " +
           "FROM users u JOIN orders o ON u.id = o.user_id " +
           "WHERE o.status = 'DELIVERED' AND o.created_at >= :startDate AND o.created_at < :endDate " +
           "GROUP BY u.id, u.first_name, u.last_name, u.email " +
           "ORDER BY 6 DESC", nativeQuery = true)
    List<Object[]> topCustomersBySpendInRange(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable);

    @Query(value = "SELECT COUNT(DISTINCT o.user_id) FROM orders o " +
           "WHERE o.created_at >= :startDate AND o.created_at < :endDate " +
           "AND o.status != 'CANCELLED'", nativeQuery = true)
    Long countDistinctCustomersInRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COALESCE(AVG(item_count), 0) FROM " +
           "(SELECT COUNT(oi.id) AS item_count FROM orders o " +
           "JOIN order_items oi ON o.id = oi.order_id " +
           "WHERE o.status != 'CANCELLED' GROUP BY o.id) sub", nativeQuery = true)
    Double averageItemsPerOrder();

    // Unpaginated export query with eager item loading
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:dateFrom IS NULL OR o.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR o.createdAt <= :dateTo) AND " +
           "(:minAmount IS NULL OR o.totalAmount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR o.totalAmount <= :maxAmount) " +
           "ORDER BY o.createdAt DESC")
    List<Order> findAllWithFilters(
            @Param("status") Order.OrderStatus status,
            @Param("dateFrom") java.time.LocalDateTime dateFrom,
            @Param("dateTo") java.time.LocalDateTime dateTo,
            @Param("minAmount") java.math.BigDecimal minAmount,
            @Param("maxAmount") java.math.BigDecimal maxAmount);
}
