package com.kalon.specification;

import com.kalon.entity.Category;
import com.kalon.entity.Product;
import com.kalon.entity.ProductVariant;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> buildSpecification(
            String keyword,
            Long categoryId,
            String categorySlug,
            String gender,
            List<String> sizes,
            List<String> colors,
            List<String> brands,
            List<String> materials,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            Boolean onSale,
            Boolean featured,
            Boolean newArrival,
            Boolean bestSeller
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter active products
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));

            // Keyword search - search in name, description, brand
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchTerm = "%" + keyword.toLowerCase().trim() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), searchTerm);
                Predicate descPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchTerm);
                Predicate brandPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("brandName")), searchTerm);
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate, brandPredicate));
            }

            // Category filter by ID
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            // Category filter by slug
            if (categorySlug != null && !categorySlug.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("slug"), categorySlug));
            }

            // Gender filter
            if (gender != null && !gender.trim().isEmpty()) {
                try {
                    Category.GenderType genderType = Category.GenderType.valueOf(gender.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("category").get("genderType"), genderType));
                } catch (IllegalArgumentException e) {
                    // Invalid gender type, ignore filter
                }
            }

            // Price range filter
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // Brand filter (multiple brands)
            if (brands != null && !brands.isEmpty()) {
                List<Predicate> brandPredicates = new ArrayList<>();
                for (String brand : brands) {
                    brandPredicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("brandName")), brand.toLowerCase()));
                }
                predicates.add(criteriaBuilder.or(brandPredicates.toArray(new Predicate[0])));
            }

            // Material filter (multiple materials)
            if (materials != null && !materials.isEmpty()) {
                List<Predicate> materialPredicates = new ArrayList<>();
                for (String material : materials) {
                    materialPredicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("material")), "%" + material.toLowerCase() + "%"));
                }
                predicates.add(criteriaBuilder.or(materialPredicates.toArray(new Predicate[0])));
            }

            // On sale filter (has discount)
            if (onSale != null && onSale) {
                predicates.add(criteriaBuilder.greaterThan(root.get("discountPercentage"), 0));
            }

            // Featured filter
            if (featured != null && featured) {
                predicates.add(criteriaBuilder.isTrue(root.get("isFeatured")));
            }

            // New arrival filter
            if (newArrival != null && newArrival) {
                predicates.add(criteriaBuilder.isTrue(root.get("isNewArrival")));
            }

            // Best seller filter
            if (bestSeller != null && bestSeller) {
                predicates.add(criteriaBuilder.isTrue(root.get("isBestSeller")));
            }

            // Size filter - requires join with variants
            if (sizes != null && !sizes.isEmpty()) {
                Subquery<Long> sizeSubquery = query.subquery(Long.class);
                Root<ProductVariant> variantRoot = sizeSubquery.from(ProductVariant.class);
                sizeSubquery.select(variantRoot.get("product").get("id"));

                List<Predicate> sizePredicates = new ArrayList<>();
                for (String size : sizes) {
                    sizePredicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(variantRoot.get("size")), size.toLowerCase()));
                }

                Predicate sizePredicate = criteriaBuilder.or(sizePredicates.toArray(new Predicate[0]));
                Predicate availablePredicate = criteriaBuilder.isTrue(variantRoot.get("isAvailable"));

                if (inStock != null && inStock) {
                    Predicate stockPredicate = criteriaBuilder.greaterThan(variantRoot.get("stockQuantity"), 0);
                    sizeSubquery.where(sizePredicate, availablePredicate, stockPredicate);
                } else {
                    sizeSubquery.where(sizePredicate, availablePredicate);
                }

                predicates.add(root.get("id").in(sizeSubquery));
            }

            // Color filter - requires join with variants
            if (colors != null && !colors.isEmpty()) {
                Subquery<Long> colorSubquery = query.subquery(Long.class);
                Root<ProductVariant> variantRoot = colorSubquery.from(ProductVariant.class);
                colorSubquery.select(variantRoot.get("product").get("id"));

                List<Predicate> colorPredicates = new ArrayList<>();
                for (String color : colors) {
                    colorPredicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(variantRoot.get("color")), color.toLowerCase()));
                }

                Predicate colorPredicate = criteriaBuilder.or(colorPredicates.toArray(new Predicate[0]));
                Predicate availablePredicate = criteriaBuilder.isTrue(variantRoot.get("isAvailable"));

                if (inStock != null && inStock) {
                    Predicate stockPredicate = criteriaBuilder.greaterThan(variantRoot.get("stockQuantity"), 0);
                    colorSubquery.where(colorPredicate, availablePredicate, stockPredicate);
                } else {
                    colorSubquery.where(colorPredicate, availablePredicate);
                }

                predicates.add(root.get("id").in(colorSubquery));
            }

            // In stock filter (any variant in stock)
            if (inStock != null && inStock && (sizes == null || sizes.isEmpty()) && (colors == null || colors.isEmpty())) {
                Subquery<Long> stockSubquery = query.subquery(Long.class);
                Root<ProductVariant> variantRoot = stockSubquery.from(ProductVariant.class);
                stockSubquery.select(variantRoot.get("product").get("id"));
                stockSubquery.where(
                    criteriaBuilder.isTrue(variantRoot.get("isAvailable")),
                    criteriaBuilder.greaterThan(variantRoot.get("stockQuantity"), 0)
                );
                predicates.add(root.get("id").in(stockSubquery));
            }

            // Make query distinct to avoid duplicates from joins
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
