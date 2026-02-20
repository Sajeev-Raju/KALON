package com.kalon.repository;

import com.kalon.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"product", "product.images", "product.variants", "product.category"})
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId")
    List<Wishlist> findByUserIdWithProducts(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"product", "product.images", "product.variants", "product.category"})
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.product.isActive = true")
    Page<Wishlist> findActiveByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.user.id = :userId AND w.product.id = :productId")
    void deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    long countByUserId(Long userId);
}
