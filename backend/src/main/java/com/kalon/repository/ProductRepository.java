package com.kalon.repository;

import com.kalon.entity.Product;
import com.kalon.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);

    List<Product> findByIsActiveTrue();

    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);
    Long countByCategoryIdAndIsActiveTrue(Long categoryId);

    Page<Product> findByCategoryAndIsActiveTrue(Category category, Pageable pageable);

    List<Product> findByIsFeaturedTrueAndIsActiveTrue();

    List<Product> findByIsNewArrivalTrueAndIsActiveTrue();

    List<Product> findByIsBestSellerTrueAndIsActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.brandName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchProductsAdmin(@Param("keyword") String keyword, Pageable pageable);

    // Admin filter query with optional search, price range, and category
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.brandName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> findWithAdminFilters(
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.category.id = :categoryId " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findByCategoryWithPriceRange(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v WHERE p.isActive = true " +
           "AND p.category.id = :categoryId AND v.size = :size")
    Page<Product> findByCategoryAndSize(
            @Param("categoryId") Long categoryId,
            @Param("size") String size,
            Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v WHERE p.isActive = true " +
           "AND p.category.id = :categoryId AND v.color = :color")
    Page<Product> findByCategoryAndColor(
            @Param("categoryId") Long categoryId,
            @Param("color") String color,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.genderType = :genderType AND p.isActive = true")
    Page<Product> findByGenderType(@Param("genderType") Category.GenderType genderType, Pageable pageable);

    // For autocomplete suggestions
    @Query("SELECT DISTINCT p.name FROM Product p WHERE p.isActive = true AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.name")
    List<String> findProductNameSuggestions(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT p.brandName FROM Product p WHERE p.isActive = true AND p.brandName IS NOT NULL " +
           "AND LOWER(p.brandName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.brandName")
    List<String> findBrandSuggestions(@Param("keyword") String keyword, Pageable pageable);

    // For filter options - get all available values
    @Query("SELECT DISTINCT p.brandName FROM Product p WHERE p.isActive = true AND p.brandName IS NOT NULL ORDER BY p.brandName")
    List<String> findAllBrands();

    @Query("SELECT DISTINCT p.material FROM Product p WHERE p.isActive = true AND p.material IS NOT NULL ORDER BY p.material")
    List<String> findAllMaterials();

    @Query("SELECT DISTINCT v.size FROM ProductVariant v JOIN v.product p WHERE p.isActive = true AND v.isAvailable = true ORDER BY v.size")
    List<String> findAllAvailableSizes();

    @Query("SELECT DISTINCT v.color FROM ProductVariant v JOIN v.product p WHERE p.isActive = true AND v.isAvailable = true ORDER BY v.color")
    List<String> findAllAvailableColors();

    @Query("SELECT MIN(p.price) FROM Product p WHERE p.isActive = true")
    BigDecimal findMinPrice();

    @Query("SELECT MAX(p.price) FROM Product p WHERE p.isActive = true")
    BigDecimal findMaxPrice();

    Page<Product> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    // ==================== FULL-TEXT SEARCH ====================

    /**
     * Full-text search with relevance ranking using PostgreSQL tsvector
     * Searches across name (weight A), brand (B), description (C), material (D)
     */
    @Query(value = "SELECT p.*, ts_rank(p.search_vector, plainto_tsquery('english', :query)) as rank " +
           "FROM products p " +
           "WHERE p.is_active = true AND p.search_vector @@ plainto_tsquery('english', :query) " +
           "ORDER BY rank DESC",
           countQuery = "SELECT count(*) FROM products p WHERE p.is_active = true AND p.search_vector @@ plainto_tsquery('english', :query)",
           nativeQuery = true)
    Page<Product> fullTextSearch(@Param("query") String query, Pageable pageable);

    /**
     * Full-text search with phrase matching for exact phrases
     */
    @Query(value = "SELECT p.*, ts_rank(p.search_vector, phraseto_tsquery('english', :query)) as rank " +
           "FROM products p " +
           "WHERE p.is_active = true AND p.search_vector @@ phraseto_tsquery('english', :query) " +
           "ORDER BY rank DESC",
           countQuery = "SELECT count(*) FROM products p WHERE p.is_active = true AND p.search_vector @@ phraseto_tsquery('english', :query)",
           nativeQuery = true)
    Page<Product> phraseSearch(@Param("query") String query, Pageable pageable);

    // ==================== FUZZY MATCHING ====================

    /**
     * Fuzzy search using trigram similarity (handles typos)
     * Returns products where name or brand has similarity > 0.15
     */
    @Query(value = "SELECT p.*, GREATEST(similarity(p.name, :query), COALESCE(similarity(p.brand_name, :query), 0)) as sim " +
           "FROM products p " +
           "WHERE p.is_active = true AND " +
           "(similarity(p.name, :query) > 0.15 OR similarity(p.brand_name, :query) > 0.15) " +
           "ORDER BY sim DESC",
           countQuery = "SELECT count(*) FROM products p WHERE p.is_active = true AND (similarity(p.name, :query) > 0.15 OR similarity(p.brand_name, :query) > 0.15)",
           nativeQuery = true)
    Page<Product> fuzzySearch(@Param("query") String query, Pageable pageable);

    /**
     * Combined search: full-text + fuzzy fallback with unified ranking
     * First tries full-text, then falls back to fuzzy for typo tolerance
     */
    @Query(value = "SELECT DISTINCT p.*, " +
           "CASE WHEN p.search_vector @@ plainto_tsquery('english', :query) " +
           "     THEN ts_rank(p.search_vector, plainto_tsquery('english', :query)) * 2 " +
           "     ELSE GREATEST(similarity(p.name, :query), COALESCE(similarity(p.brand_name, :query), 0)) " +
           "END as combined_rank " +
           "FROM products p " +
           "WHERE p.is_active = true AND " +
           "(p.search_vector @@ plainto_tsquery('english', :query) OR " +
           " similarity(p.name, :query) > 0.15 OR similarity(p.brand_name, :query) > 0.15) " +
           "ORDER BY combined_rank DESC",
           countQuery = "SELECT count(DISTINCT p.id) FROM products p WHERE p.is_active = true AND " +
                       "(p.search_vector @@ plainto_tsquery('english', :query) OR " +
                       "similarity(p.name, :query) > 0.15 OR similarity(p.brand_name, :query) > 0.15)",
           nativeQuery = true)
    Page<Product> advancedSearch(@Param("query") String query, Pageable pageable);

    /**
     * Admin variant of advanced search — includes inactive products
     */
    @Query(value = "SELECT DISTINCT p.*, " +
           "CASE WHEN p.search_vector @@ plainto_tsquery('english', :query) " +
           "     THEN ts_rank(p.search_vector, plainto_tsquery('english', :query)) * 2 " +
           "     ELSE GREATEST(similarity(p.name, :query), COALESCE(similarity(p.brand_name, :query), 0)) " +
           "END as combined_rank " +
           "FROM products p " +
           "WHERE p.search_vector @@ plainto_tsquery('english', :query) OR " +
           " similarity(p.name, :query) > 0.15 OR similarity(p.brand_name, :query) > 0.15 " +
           "ORDER BY combined_rank DESC",
           countQuery = "SELECT count(DISTINCT p.id) FROM products p WHERE " +
                       "p.search_vector @@ plainto_tsquery('english', :query) OR " +
                       "similarity(p.name, :query) > 0.15 OR similarity(p.brand_name, :query) > 0.15",
           nativeQuery = true)
    Page<Product> advancedSearchAdmin(@Param("query") String query, Pageable pageable);

    // ==================== FACETED SEARCH (FILTER COUNTS) ====================

    /**
     * Get size counts for faceted search
     */
    @Query(value = "SELECT v.size as filterValue, COUNT(DISTINCT p.id) as count " +
           "FROM products p " +
           "JOIN product_variants v ON v.product_id = p.id " +
           "WHERE p.is_active = true AND v.is_available = true " +
           "GROUP BY v.size ORDER BY count DESC",
           nativeQuery = true)
    List<Object[]> getSizeCountsRaw();

    /**
     * Get color counts for faceted search
     */
    @Query(value = "SELECT v.color as filterValue, COUNT(DISTINCT p.id) as count " +
           "FROM products p " +
           "JOIN product_variants v ON v.product_id = p.id " +
           "WHERE p.is_active = true AND v.is_available = true " +
           "GROUP BY v.color ORDER BY count DESC",
           nativeQuery = true)
    List<Object[]> getColorCountsRaw();

    /**
     * Get brand counts for faceted search
     */
    @Query(value = "SELECT p.brand_name as filterValue, COUNT(*) as count " +
           "FROM products p " +
           "WHERE p.is_active = true AND p.brand_name IS NOT NULL " +
           "GROUP BY p.brand_name ORDER BY count DESC",
           nativeQuery = true)
    List<Object[]> getBrandCountsRaw();

    /**
     * Get material counts for faceted search
     */
    @Query(value = "SELECT p.material as filterValue, COUNT(*) as count " +
           "FROM products p " +
           "WHERE p.is_active = true AND p.material IS NOT NULL " +
           "GROUP BY p.material ORDER BY count DESC",
           nativeQuery = true)
    List<Object[]> getMaterialCountsRaw();

    /**
     * Get category counts for faceted search
     */
    @Query(value = "SELECT c.name as filterValue, COUNT(p.id) as count " +
           "FROM products p " +
           "JOIN categories c ON p.category_id = c.id " +
           "WHERE p.is_active = true AND c.is_active = true " +
           "GROUP BY c.id, c.name ORDER BY count DESC",
           nativeQuery = true)
    List<Object[]> getCategoryCountsRaw();

    /**
     * Get price range stats
     */
    @Query(value = "SELECT MIN(p.price) as minPrice, MAX(p.price) as maxPrice, AVG(p.price) as avgPrice " +
           "FROM products p WHERE p.is_active = true",
           nativeQuery = true)
    Object[] getPriceRangeStats();

    /**
     * Count products on sale
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.discountPercentage > 0")
    Long countOnSale();

    /**
     * Count products in stock
     */
    @Query(value = "SELECT COUNT(DISTINCT p.id) FROM products p " +
           "JOIN product_variants v ON v.product_id = p.id " +
           "WHERE p.is_active = true AND v.is_available = true AND v.stock_quantity > 0",
           nativeQuery = true)
    Long countInStock();
}
