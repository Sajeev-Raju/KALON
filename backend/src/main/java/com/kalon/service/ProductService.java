package com.kalon.service;

import com.kalon.dto.*;
import com.kalon.entity.Category;
import com.kalon.entity.Product;
import com.kalon.entity.ProductImage;
import com.kalon.entity.ProductVariant;
import com.kalon.repository.CategoryRepository;
import com.kalon.repository.ProductRepository;
import com.kalon.repository.ProductVariantRepository;
import com.kalon.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final com.kalon.repository.ProductImageRepository productImageRepository;
    private final WishlistService wishlistService;
    private final StockMovementService stockMovementService;

    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toDTO);
    }

    public Page<ProductDTO> searchProductsAdmin(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findAll(pageable).map(this::toDTO);
        }
        return productRepository.searchProductsAdmin(keyword.trim(), pageable).map(this::toDTO);
    }

    public Page<ProductDTO> advancedSearchAdmin(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findAll(pageable).map(this::toDTO);
        }
        return productRepository.advancedSearchAdmin(keyword.trim(), pageable).map(this::toDTO);
    }

    public Page<ProductDTO> getProductsWithAdminFilters(String keyword, BigDecimal minPrice,
                                                         BigDecimal maxPrice, Long categoryId,
                                                         Pageable pageable) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return productRepository.findWithAdminFilters(trimmedKeyword, minPrice, maxPrice, categoryId, pageable)
                .map(this::toDTO);
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toDTO(product);
    }

    public ProductDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toDTO(product);
    }

    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable).map(this::toDTO);
    }

    public Page<ProductDTO> getProductsByCategorySlug(String slug, Pageable pageable) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return productRepository.findByCategoryAndIsActiveTrue(category, pageable).map(this::toDTO);
    }

    public Page<ProductDTO> getProductsByGender(String gender, Pageable pageable) {
        Category.GenderType genderType = Category.GenderType.valueOf(gender.toUpperCase());
        return productRepository.findByGenderType(genderType, pageable).map(this::toDTO);
    }

    public Page<ProductDTO> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchProducts(keyword, pageable).map(this::toDTO);
    }

    /**
     * Advanced search with full-text search and fuzzy matching fallback
     * Uses PostgreSQL tsvector for relevance ranking and pg_trgm for typo tolerance
     */
    public Page<ProductDTO> advancedSearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable).map(this::toDTO);
        }
        return productRepository.advancedSearch(keyword.trim(), pageable).map(this::toDTO);
    }

    /**
     * Full-text search only (no fuzzy matching)
     */
    public Page<ProductDTO> fullTextSearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable).map(this::toDTO);
        }
        return productRepository.fullTextSearch(keyword.trim(), pageable).map(this::toDTO);
    }

    /**
     * Fuzzy search for typo tolerance
     */
    public Page<ProductDTO> fuzzySearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable).map(this::toDTO);
        }
        return productRepository.fuzzySearch(keyword.trim(), pageable).map(this::toDTO);
    }

    public List<ProductDTO> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrueAndIsActiveTrue()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ProductDTO> getNewArrivals() {
        return productRepository.findByIsNewArrivalTrueAndIsActiveTrue()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ProductDTO> getBestSellers() {
        return productRepository.findByIsBestSellerTrueAndIsActiveTrue()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Page<ProductDTO> filterProducts(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByCategoryWithPriceRange(categoryId, minPrice, maxPrice, pageable).map(this::toDTO);
    }

    /**
     * Advanced search and filter with all options combined
     * Uses full-text search with relevance ranking when keyword is provided
     */
    public Page<ProductDTO> searchAndFilter(ProductSearchRequest request) {
        // Build sort
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        String sortDir = request.getSortDir() != null ? request.getSortDir() : "desc";
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);

        // Build pageable
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 12;
        Pageable pageable = PageRequest.of(page, size, sort);

        // Check if we have only a keyword search with no filters
        boolean hasFilters = request.getCategoryId() != null ||
            request.getCategorySlug() != null ||
            request.getGender() != null ||
            (request.getSizes() != null && !request.getSizes().isEmpty()) ||
            (request.getColors() != null && !request.getColors().isEmpty()) ||
            (request.getBrands() != null && !request.getBrands().isEmpty()) ||
            (request.getMaterials() != null && !request.getMaterials().isEmpty()) ||
            request.getMinPrice() != null ||
            request.getMaxPrice() != null ||
            Boolean.TRUE.equals(request.getInStock()) ||
            Boolean.TRUE.equals(request.getOnSale()) ||
            Boolean.TRUE.equals(request.getFeatured()) ||
            Boolean.TRUE.equals(request.getNewArrival()) ||
            Boolean.TRUE.equals(request.getBestSeller());

        String keyword = request.getKeyword();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        // If only keyword search with no filters and sorting by relevance, use advanced search
        if (hasKeyword && !hasFilters && "createdAt".equals(sortBy)) {
            // Use advanced search with relevance ranking
            Pageable unsortedPageable = PageRequest.of(page, size);
            return productRepository.advancedSearch(keyword.trim(), unsortedPageable).map(this::toDTO);
        }

        // Build specification for combined search + filters
        Specification<Product> spec = ProductSpecification.buildSpecification(
            request.getKeyword(),
            request.getCategoryId(),
            request.getCategorySlug(),
            request.getGender(),
            request.getSizes(),
            request.getColors(),
            request.getBrands(),
            request.getMaterials(),
            request.getMinPrice(),
            request.getMaxPrice(),
            request.getInStock(),
            request.getOnSale(),
            request.getFeatured(),
            request.getNewArrival(),
            request.getBestSeller()
        );

        return productRepository.findAll(spec, pageable).map(this::toDTO);
    }

    /**
     * Get all available filter options
     */
    public FilterOptionsDTO getFilterOptions() {
        return FilterOptionsDTO.builder()
            .sizes(productRepository.findAllAvailableSizes())
            .colors(productRepository.findAllAvailableColors())
            .brands(productRepository.findAllBrands())
            .materials(productRepository.findAllMaterials())
            .minPrice(productRepository.findMinPrice())
            .maxPrice(productRepository.findMaxPrice())
            .genders(Arrays.asList("MEN", "WOMEN", "UNISEX", "KIDS"))
            .build();
    }

    /**
     * Get faceted filter options with counts
     */
    public FacetedFilterOptionsDTO getFacetedFilterOptions() {
        // Convert raw query results to FilterCount objects
        List<FacetedFilterOptionsDTO.FilterCount> sizes = convertToFilterCounts(productRepository.getSizeCountsRaw());
        List<FacetedFilterOptionsDTO.FilterCount> colors = convertToFilterCounts(productRepository.getColorCountsRaw());
        List<FacetedFilterOptionsDTO.FilterCount> brands = convertToFilterCounts(productRepository.getBrandCountsRaw());
        List<FacetedFilterOptionsDTO.FilterCount> materials = convertToFilterCounts(productRepository.getMaterialCountsRaw());
        List<FacetedFilterOptionsDTO.FilterCount> categories = convertToFilterCounts(productRepository.getCategoryCountsRaw());

        // Get price range
        Object[] priceStats = productRepository.getPriceRangeStats();
        FacetedFilterOptionsDTO.PriceRange priceRange = null;
        if (priceStats != null && priceStats.length > 0 && priceStats[0] != null) {
            Object[] stats = priceStats;
            priceRange = FacetedFilterOptionsDTO.PriceRange.builder()
                .min(stats[0] != null ? new BigDecimal(stats[0].toString()) : BigDecimal.ZERO)
                .max(stats[1] != null ? new BigDecimal(stats[1].toString()) : BigDecimal.ZERO)
                .avg(stats[2] != null ? new BigDecimal(stats[2].toString()) : BigDecimal.ZERO)
                .build();
        }

        // Get counts
        Long onSaleCount = productRepository.countOnSale();
        Long inStockCount = productRepository.countInStock();
        Long totalProducts = productRepository.count();

        return FacetedFilterOptionsDTO.builder()
            .sizes(sizes)
            .colors(colors)
            .brands(brands)
            .materials(materials)
            .categories(categories)
            .genders(Arrays.asList("MEN", "WOMEN", "UNISEX", "KIDS"))
            .priceRange(priceRange)
            .onSaleCount(onSaleCount)
            .inStockCount(inStockCount)
            .totalProducts(totalProducts)
            .build();
    }

    private List<FacetedFilterOptionsDTO.FilterCount> convertToFilterCounts(List<Object[]> rawResults) {
        List<FacetedFilterOptionsDTO.FilterCount> counts = new ArrayList<>();
        if (rawResults != null) {
            for (Object[] row : rawResults) {
                if (row[0] != null) {
                    counts.add(FacetedFilterOptionsDTO.FilterCount.builder()
                        .value(row[0].toString())
                        .count(((Number) row[1]).longValue())
                        .build());
                }
            }
        }
        return counts;
    }

    /**
     * Get search suggestions for autocomplete
     */
    public SearchSuggestionDTO getSearchSuggestions(String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return SearchSuggestionDTO.builder()
                .products(List.of())
                .brands(List.of())
                .categories(List.of())
                .build();
        }

        Pageable limit = PageRequest.of(0, 5);

        List<String> productNames = productRepository.findProductNameSuggestions(keyword, limit);
        List<String> brandNames = productRepository.findBrandSuggestions(keyword, limit);
        List<String> categoryNames = categoryRepository.findCategoryNameSuggestions(keyword, limit);

        return SearchSuggestionDTO.builder()
            .products(productNames)
            .brands(brandNames)
            .categories(categoryNames)
            .build();
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(dto.getName())
                .slug(dto.getSlug())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .originalPrice(dto.getOriginalPrice())
                .discountPercentage(dto.getDiscountPercentage())
                .category(category)
                .brandName(dto.getBrandName())
                .material(dto.getMaterial())
                .careInstructions(dto.getCareInstructions())
                .countryOfOrigin(dto.getCountryOfOrigin())
                .isFeatured(dto.isFeatured())
                .isNewArrival(dto.isNewArrival())
                .isBestSeller(dto.isBestSeller())
                .isReturnable(dto.isReturnable())
                .metaTitle(dto.getMetaTitle())
                .metaDescription(dto.getMetaDescription())
                .metaKeywords(dto.getMetaKeywords())
                .ogImage(dto.getOgImage())
                .isActive(true)
                .build();

        product = productRepository.save(product);

        // Save images if provided
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            for (int i = 0; i < dto.getImages().size(); i++) {
                ProductImageDTO imageDTO = dto.getImages().get(i);
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(imageDTO.getImageUrl())
                        .altText(imageDTO.getAltText() != null ? imageDTO.getAltText() : product.getName())
                        .isPrimary(i == 0 || imageDTO.isPrimary())
                        .displayOrder(i)
                        .build();
                product.getImages().add(image);
            }
            product = productRepository.save(product);
        }

        // Save variants if provided
        if (dto.getVariants() != null && !dto.getVariants().isEmpty()) {
            for (ProductVariantDTO variantDTO : dto.getVariants()) {
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .size(variantDTO.getSize())
                        .color(variantDTO.getColor())
                        .colorCode(variantDTO.getColorCode())
                        .sku(variantDTO.getSku())
                        .stockQuantity(variantDTO.getStockQuantity() != null ? variantDTO.getStockQuantity() : 0)
                        .isAvailable(variantDTO.isAvailable())
                        .reorderPoint(variantDTO.getReorderPoint())
                        .build();
                product.getVariants().add(variant);
            }
            product = productRepository.save(product);
        }

        return toDTO(product);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getSlug() != null) product.setSlug(dto.getSlug());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getOriginalPrice() != null) product.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getDiscountPercentage() != null) product.setDiscountPercentage(dto.getDiscountPercentage());
        if (dto.getBrandName() != null) product.setBrandName(dto.getBrandName());
        if (dto.getMaterial() != null) product.setMaterial(dto.getMaterial());
        if (dto.getCareInstructions() != null) product.setCareInstructions(dto.getCareInstructions());
        if (dto.getCountryOfOrigin() != null) product.setCountryOfOrigin(dto.getCountryOfOrigin());
        
        // Update boolean flags
        product.setFeatured(dto.isFeatured());
        product.setNewArrival(dto.isNewArrival());
        product.setBestSeller(dto.isBestSeller());
        product.setReturnable(dto.isReturnable());
        if (dto.getMetaTitle() != null) product.setMetaTitle(dto.getMetaTitle());
        if (dto.getMetaDescription() != null) product.setMetaDescription(dto.getMetaDescription());
        if (dto.getMetaKeywords() != null) product.setMetaKeywords(dto.getMetaKeywords());
        if (dto.getOgImage() != null) product.setOgImage(dto.getOgImage());

        product = productRepository.save(product);
        
        // Update images if provided
        if (dto.getImages() != null) {
            product.getImages().clear();
            for (int i = 0; i < dto.getImages().size(); i++) {
                ProductImageDTO imageDTO = dto.getImages().get(i);
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(imageDTO.getImageUrl())
                        .altText(imageDTO.getAltText() != null ? imageDTO.getAltText() : product.getName())
                        .isPrimary(i == 0 || imageDTO.isPrimary())
                        .displayOrder(i)
                        .build();
                product.getImages().add(image);
            }
            product = productRepository.save(product);
        }

        // Update variants if provided
        if (dto.getVariants() != null) {
            // Snapshot old variant stock for movement tracking
            Map<String, Integer> oldStockMap = new HashMap<>();
            for (ProductVariant oldVariant : product.getVariants()) {
                String key = oldVariant.getSize() + "|" + oldVariant.getColor();
                oldStockMap.put(key, oldVariant.getStockQuantity());
            }

            product.getVariants().clear();
            productRepository.saveAndFlush(product);
            for (ProductVariantDTO variantDTO : dto.getVariants()) {
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .size(variantDTO.getSize())
                        .color(variantDTO.getColor())
                        .colorCode(variantDTO.getColorCode())
                        .sku(variantDTO.getSku())
                        .stockQuantity(variantDTO.getStockQuantity() != null ? variantDTO.getStockQuantity() : 0)
                        .isAvailable(variantDTO.isAvailable())
                        .reorderPoint(variantDTO.getReorderPoint())
                        .build();
                product.getVariants().add(variant);
            }
            product = productRepository.save(product);

            // Record stock movement diffs for any changes
            for (ProductVariant newVariant : product.getVariants()) {
                String key = newVariant.getSize() + "|" + newVariant.getColor();
                Integer oldStock = oldStockMap.remove(key);
                int before = oldStock != null ? oldStock : 0;
                int after = newVariant.getStockQuantity();
                int diff = after - before;
                if (diff != 0) {
                    stockMovementService.recordMovement(newVariant, product,
                            com.kalon.entity.StockMovement.MovementType.MANUAL_ADJUSTMENT,
                            diff, before, after,
                            com.kalon.entity.StockMovement.ReferenceType.ADMIN, product.getId(),
                            "Product update — variant " + newVariant.getSize() + "/" + newVariant.getColor());
                }
            }
            // Record movements for removed variants (stock went to 0)
            for (Map.Entry<String, Integer> removed : oldStockMap.entrySet()) {
                if (removed.getValue() != 0) {
                    // Variant was removed — we can't reference it anymore, but we log against the product
                    // We use a dummy approach: log a note-only movement on the first new variant if available
                    if (!product.getVariants().isEmpty()) {
                        ProductVariant firstVariant = product.getVariants().iterator().next();
                        stockMovementService.recordMovement(firstVariant, product,
                                com.kalon.entity.StockMovement.MovementType.MANUAL_ADJUSTMENT,
                                -removed.getValue(), removed.getValue(), 0,
                                com.kalon.entity.StockMovement.ReferenceType.ADMIN, product.getId(),
                                "Variant removed (" + removed.getKey().replace("|", "/") + ") during product update");
                    }
                }
            }
        }

        return toDTO(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new com.kalon.exception.ResourceNotFoundException("Product not found"));
        product.setActive(false);
        productRepository.save(product);

        // Remove from all wishlists
        wishlistService.removeProductFromAllWishlists(id);
    }

    private ProductDTO toDTO(Product product) {
        List<String> sizes = productVariantRepository.findDistinctSizesByProductId(product.getId());
        List<String> colors = productVariantRepository.findDistinctColorsByProductId(product.getId());

        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discountPercentage(product.getDiscountPercentage())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .brandName(product.getBrandName())
                .material(product.getMaterial())
                .careInstructions(product.getCareInstructions())
                .countryOfOrigin(product.getCountryOfOrigin())
                .isFeatured(product.isFeatured())
                .isNewArrival(product.isNewArrival())
                .isBestSeller(product.isBestSeller())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .isReturnable(product.isReturnable())
                .images(product.getImages().stream().map(this::toImageDTO).collect(Collectors.toList()))
                .variants(product.getVariants().stream().map(this::toVariantDTO).collect(Collectors.toList()))
                .availableSizes(sizes)
                .availableColors(colors)
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .metaKeywords(product.getMetaKeywords())
                .ogImage(product.getOgImage())
                .build();
    }

    private ProductImageDTO toImageDTO(ProductImage image) {
        return ProductImageDTO.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .isPrimary(image.isPrimary())
                .displayOrder(image.getDisplayOrder())
                .build();
    }

    private ProductVariantDTO toVariantDTO(ProductVariant variant) {
        return ProductVariantDTO.builder()
                .id(variant.getId())
                .size(variant.getSize())
                .color(variant.getColor())
                .colorCode(variant.getColorCode())
                .sku(variant.getSku())
                .stockQuantity(variant.getStockQuantity())
                .isAvailable(variant.isAvailable())
                .reorderPoint(variant.getReorderPoint())
                .build();
    }
}
