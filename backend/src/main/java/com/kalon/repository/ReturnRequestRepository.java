package com.kalon.repository;

import com.kalon.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    @EntityGraph(attributePaths = {"order", "orderItem", "orderItem.product"})
    List<ReturnRequest> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "orderItem", "orderItem.product", "order.user"})
    @Query("SELECT r FROM ReturnRequest r WHERE r.order.user.id = :userId ORDER BY r.createdAt DESC")
    List<ReturnRequest> findByOrderUserId(@Param("userId") Long userId);

    List<ReturnRequest> findByStatus(ReturnRequest.ReturnStatus status);

    @EntityGraph(attributePaths = {"order", "orderItem", "orderItem.product"})
    Page<ReturnRequest> findByStatus(ReturnRequest.ReturnStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "orderItem", "orderItem.product"})
    Page<ReturnRequest> findByRequestType(ReturnRequest.RequestType requestType, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "orderItem", "orderItem.product"})
    Page<ReturnRequest> findByStatusAndRequestType(ReturnRequest.ReturnStatus status, ReturnRequest.RequestType requestType, Pageable pageable);

    Optional<ReturnRequest> findByOrderItemId(Long orderItemId);

    @Query("SELECT r.orderItem.id FROM ReturnRequest r WHERE r.orderItem.id IN :orderItemIds")
    List<Long> findOrderItemIdsWithReturnRequests(@Param("orderItemIds") List<Long> orderItemIds);

    Long countByStatus(ReturnRequest.ReturnStatus status);

    @EntityGraph(attributePaths = {"order", "orderItem", "orderItem.product"})
    @Query("SELECT r FROM ReturnRequest r WHERE " +
           "LOWER(r.order.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.orderItem.productName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ReturnRequest> searchReturnRequests(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "orderItem", "orderItem.product"})
    @Query("SELECT r FROM ReturnRequest r")
    Page<ReturnRequest> findAllWithDetails(Pageable pageable);

    @EntityGraph(attributePaths = {"order", "orderItem", "orderItem.product"})
    @Query("SELECT r FROM ReturnRequest r WHERE r.id = :id")
    Optional<ReturnRequest> findByIdWithDetails(@Param("id") Long id);

    // ─── Analytics queries ───

    @Query(value = "SELECT COALESCE(SUM(r.refund_amount), 0) FROM return_requests r " +
           "WHERE r.status = 'COMPLETED' " +
           "AND r.updated_at >= :startDate AND r.updated_at < :endDate", nativeQuery = true)
    BigDecimal totalRefundsInRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM return_requests r " +
           "WHERE r.status = 'COMPLETED' " +
           "AND r.updated_at >= :startDate AND r.updated_at < :endDate", nativeQuery = true)
    Long countCompletedReturnsInRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT oi.product_id, oi.product_name, COUNT(r.id) " +
           "FROM return_requests r JOIN order_items oi ON r.order_item_id = oi.id " +
           "WHERE r.status = 'COMPLETED' " +
           "GROUP BY oi.product_id, oi.product_name " +
           "ORDER BY COUNT(r.id) DESC", nativeQuery = true)
    List<Object[]> returnCountByProduct(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM return_requests r WHERE r.status = 'COMPLETED'", nativeQuery = true)
    Long countAllCompletedReturns();
}
