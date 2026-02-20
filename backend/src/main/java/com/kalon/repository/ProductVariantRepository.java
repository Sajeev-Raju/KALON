package com.kalon.repository;

import com.kalon.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    Optional<ProductVariant> findByProductIdAndSizeAndColor(Long productId, String size, String color);
    Optional<ProductVariant> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.size = :size AND v.color = :color")
    Optional<ProductVariant> findByProductIdAndSizeAndColorForUpdate(
            @Param("productId") Long productId, @Param("size") String size, @Param("color") String color);

    @Query("SELECT DISTINCT v.size FROM ProductVariant v WHERE v.product.id = :productId")
    List<String> findDistinctSizesByProductId(@Param("productId") Long productId);

    @Query("SELECT DISTINCT v.color FROM ProductVariant v WHERE v.product.id = :productId")
    List<String> findDistinctColorsByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(v) FROM ProductVariant v WHERE v.stockQuantity < :threshold AND v.isAvailable = true")
    Long countLowStock(@Param("threshold") int threshold);

    // ─── Analytics queries ───

    @Query(value = "SELECT v.id, p.id, p.name, v.size, v.color, v.stock_quantity, v.reorder_point " +
           "FROM product_variants v JOIN products p ON v.product_id = p.id " +
           "WHERE v.stock_quantity < :threshold AND v.is_available = true AND p.is_active = true " +
           "ORDER BY v.stock_quantity ASC", nativeQuery = true)
    List<Object[]> findLowStockVariantsWithDetails(@Param("threshold") int threshold, Pageable pageable);

    @Query(value = "SELECT v.id, p.id, p.name, v.size, v.color, v.stock_quantity, v.reorder_point " +
           "FROM product_variants v JOIN products p ON v.product_id = p.id " +
           "WHERE v.is_available = true AND p.is_active = true " +
           "AND (v.stock_quantity < :threshold OR (v.reorder_point IS NOT NULL AND v.stock_quantity < v.reorder_point)) " +
           "ORDER BY v.stock_quantity ASC", nativeQuery = true)
    List<Object[]> findVariantsBelowThresholdOrReorderPoint(@Param("threshold") int threshold);
}
