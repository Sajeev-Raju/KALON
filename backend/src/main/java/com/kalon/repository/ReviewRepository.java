package com.kalon.repository;

import com.kalon.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Fetch with user eagerly to avoid N+1
    @EntityGraph(attributePaths = {"user", "product"})
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Page<Review> findByProductIdAndIsApprovedTrue(@Param("productId") Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product"})
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Duplicate prevention: check if user already reviewed this product
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // Find existing review by user+product for edit flow
    @EntityGraph(attributePaths = {"user", "product"})
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Integer getReviewCountByProductId(@Param("productId") Long productId);

    // Admin: filtered review listing
    @EntityGraph(attributePaths = {"user", "product"})
    @Query("SELECT r FROM Review r WHERE " +
           "(:productId IS NULL OR r.product.id = :productId) AND " +
           "(:userId IS NULL OR r.user.id = :userId) AND " +
           "(:isApproved IS NULL OR r.isApproved = :isApproved) AND " +
           "(:minRating IS NULL OR r.rating >= :minRating) AND " +
           "(:maxRating IS NULL OR r.rating <= :maxRating)")
    Page<Review> findWithFilters(
            @Param("productId") Long productId,
            @Param("userId") Long userId,
            @Param("isApproved") Boolean isApproved,
            @Param("minRating") Integer minRating,
            @Param("maxRating") Integer maxRating,
            Pageable pageable);

    // Admin: findAll with eager loading
    @EntityGraph(attributePaths = {"user", "product"})
    @Query("SELECT r FROM Review r")
    Page<Review> findAllWithDetails(Pageable pageable);

    // Find by ID with eager loading
    @EntityGraph(attributePaths = {"user", "product"})
    @Query("SELECT r FROM Review r WHERE r.id = :id")
    Optional<Review> findByIdWithDetails(@Param("id") Long id);

    // ─── Analytics queries ───

    @Query("SELECT COUNT(r) FROM Review r WHERE r.isApproved = false")
    Long countUnapprovedReviews();
}
