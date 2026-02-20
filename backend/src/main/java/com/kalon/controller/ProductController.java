package com.kalon.controller;

import com.kalon.dto.*;
import com.kalon.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByCategory(categoryId, pageable)));
    }

    @GetMapping("/category/slug/{slug}")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getProductsByCategorySlug(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByCategorySlug(slug, pageable)));
    }

    @GetMapping("/gender/{gender}")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getProductsByGender(
            @PathVariable String gender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByGender(gender, pageable)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(productService.searchProducts(keyword, pageable)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getFeaturedProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getFeaturedProducts()));
    }

    @GetMapping("/new-arrivals")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getNewArrivals() {
        return ResponseEntity.ok(ApiResponse.success(productService.getNewArrivals()));
    }

    @GetMapping("/best-sellers")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getBestSellers() {
        return ResponseEntity.ok(ApiResponse.success(productService.getBestSellers()));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> filterProducts(
            @RequestParam Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.success(
                productService.filterProducts(categoryId, minPrice, maxPrice, pageable)));
    }

    /**
     * Advanced unified search and filter endpoint
     * Supports: keyword search, category, gender, sizes, colors, brands, materials,
     * price range, inStock, onSale, featured, newArrival, bestSeller
     */
    @GetMapping("/search-filter")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> searchAndFilter(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) List<String> sizes,
            @RequestParam(required = false) List<String> colors,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) List<String> materials,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean newArrival,
            @RequestParam(required = false) Boolean bestSeller,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size) {

        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword(keyword);
        request.setCategoryId(categoryId);
        request.setCategorySlug(categorySlug);
        request.setGender(gender);
        request.setSizes(sizes);
        request.setColors(colors);
        request.setBrands(brands);
        request.setMaterials(materials);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setInStock(inStock);
        request.setOnSale(onSale);
        request.setFeatured(featured);
        request.setNewArrival(newArrival);
        request.setBestSeller(bestSeller);
        request.setSortBy(sortBy);
        request.setSortDir(sortDir);
        request.setPage(page);
        request.setSize(size);

        return ResponseEntity.ok(ApiResponse.success(productService.searchAndFilter(request)));
    }

    /**
     * Get available filter options (sizes, colors, brands, etc.)
     */
    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<FilterOptionsDTO>> getFilterOptions() {
        return ResponseEntity.ok(ApiResponse.success(productService.getFilterOptions()));
    }

    /**
     * Get faceted filter options with counts
     * Returns filter options with the count of products for each option
     */
    @GetMapping("/faceted-filter-options")
    public ResponseEntity<ApiResponse<FacetedFilterOptionsDTO>> getFacetedFilterOptions() {
        return ResponseEntity.ok(ApiResponse.success(productService.getFacetedFilterOptions()));
    }

    /**
     * Advanced search endpoint with full-text search and fuzzy matching
     * Uses PostgreSQL tsvector for relevance ranking and pg_trgm for typo tolerance
     */
    @GetMapping("/advanced-search")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> advancedSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(productService.advancedSearch(keyword, pageable)));
    }

    /**
     * Get search suggestions for autocomplete
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<SearchSuggestionDTO>> getSearchSuggestions(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(productService.getSearchSuggestions(keyword)));
    }
}
