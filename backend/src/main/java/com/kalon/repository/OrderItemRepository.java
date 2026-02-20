package com.kalon.repository;

import com.kalon.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // ─── Analytics queries ───

    @Query(value = "SELECT oi.product_id, oi.product_name, c.name, " +
           "SUM(oi.quantity), SUM(oi.total_price), " +
           "COALESCE(p.average_rating, 0), COALESCE(p.review_count, 0) " +
           "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
           "JOIN products p ON oi.product_id = p.id JOIN categories c ON p.category_id = c.id " +
           "WHERE o.status = 'DELIVERED' AND o.created_at >= :startDate AND o.created_at < :endDate " +
           "GROUP BY oi.product_id, oi.product_name, c.name, p.average_rating, p.review_count " +
           "ORDER BY SUM(oi.total_price) DESC", nativeQuery = true)
    List<Object[]> topProductsByRevenueInRange(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);

    @Query(value = "SELECT oi.product_id, oi.product_name, c.name, " +
           "SUM(oi.quantity), SUM(oi.total_price), " +
           "COALESCE(p.average_rating, 0), COALESCE(p.review_count, 0) " +
           "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
           "JOIN products p ON oi.product_id = p.id JOIN categories c ON p.category_id = c.id " +
           "WHERE o.status = 'DELIVERED' AND o.created_at >= :startDate AND o.created_at < :endDate " +
           "GROUP BY oi.product_id, oi.product_name, c.name, p.average_rating, p.review_count " +
           "ORDER BY SUM(oi.quantity) DESC", nativeQuery = true)
    List<Object[]> topProductsByQuantityInRange(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 Pageable pageable);

    @Query(value = "SELECT c.id, c.name, COUNT(DISTINCT p.id), " +
           "COALESCE(SUM(oi.quantity), 0), COALESCE(SUM(oi.total_price), 0) " +
           "FROM categories c LEFT JOIN products p ON p.category_id = c.id " +
           "LEFT JOIN order_items oi ON oi.product_id = p.id " +
           "LEFT JOIN orders o ON oi.order_id = o.id AND o.status = 'DELIVERED' " +
           "AND o.created_at >= :startDate AND o.created_at < :endDate " +
           "WHERE c.is_active = true " +
           "GROUP BY c.id, c.name ORDER BY 5 DESC", nativeQuery = true)
    List<Object[]> categoryPerformanceInRange(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE oi.product_id = :productId AND o.status = 'DELIVERED'", nativeQuery = true)
    Long totalUnitsSoldForProduct(@Param("productId") Long productId);
}
