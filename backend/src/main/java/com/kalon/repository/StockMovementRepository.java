package com.kalon.repository;

import com.kalon.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductVariantIdOrderByCreatedAtDesc(Long variantId, Pageable pageable);

    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm " +
           "WHERE (:variantId IS NULL OR sm.productVariant.id = :variantId) " +
           "AND (:productId IS NULL OR sm.product.id = :productId) " +
           "AND (:movementType IS NULL OR sm.movementType = :movementType) " +
           "AND (:dateFrom IS NULL OR sm.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR sm.createdAt <= :dateTo) " +
           "ORDER BY sm.createdAt DESC")
    Page<StockMovement> findWithFilters(
            @Param("variantId") Long variantId,
            @Param("productId") Long productId,
            @Param("movementType") StockMovement.MovementType movementType,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);
}
