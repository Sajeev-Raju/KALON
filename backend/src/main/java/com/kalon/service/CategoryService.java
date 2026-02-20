package com.kalon.service;

import com.kalon.dto.CategoryDTO;
import com.kalon.entity.Category;
import com.kalon.repository.CategoryRepository;
import com.kalon.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAllActiveOrderByDisplayOrder()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<CategoryDTO> getRootCategories() {
        return categoryRepository.findByParentIsNullAndIsActiveTrue()
                .stream().map(this::toDTOWithSubcategories).collect(Collectors.toList());
    }

    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return toDTOWithSubcategories(category);
    }

    public CategoryDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return toDTOWithSubcategories(category);
    }

    public List<CategoryDTO> getCategoriesByGender(String gender) {
        Category.GenderType genderType = Category.GenderType.valueOf(gender.toUpperCase());
        return categoryRepository.findByGenderTypeAndIsActiveTrue(genderType)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<CategoryDTO> getSubcategories(Long parentId) {
        return categoryRepository.findByParentIdAndIsActiveTrue(parentId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto) {
        Category category = Category.builder()
                .name(dto.getName())
                .slug(dto.getSlug())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .genderType(dto.getGenderType() != null ?
                        Category.GenderType.valueOf(dto.getGenderType()) : null)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .isActive(true)
                .metaTitle(dto.getMetaTitle())
                .metaDescription(dto.getMetaDescription())
                .metaKeywords(dto.getMetaKeywords())
                .ogImage(dto.getOgImage())
                .build();

        if (dto.getParentId() != null) {
            Category parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        return toDTO(category);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (dto.getName() != null) category.setName(dto.getName());
        if (dto.getSlug() != null) category.setSlug(dto.getSlug());
        if (dto.getDescription() != null) category.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null) category.setImageUrl(dto.getImageUrl());
        if (dto.getGenderType() != null) {
            category.setGenderType(Category.GenderType.valueOf(dto.getGenderType()));
        }
        if (dto.getDisplayOrder() != null) category.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getMetaTitle() != null) category.setMetaTitle(dto.getMetaTitle());
        if (dto.getMetaDescription() != null) category.setMetaDescription(dto.getMetaDescription());
        if (dto.getMetaKeywords() != null) category.setMetaKeywords(dto.getMetaKeywords());
        if (dto.getOgImage() != null) category.setOgImage(dto.getOgImage());

        category = categoryRepository.save(category);
        return toDTO(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Long productCount = productRepository.countByCategoryIdAndIsActiveTrue(id);
        if (productCount > 0) {
            throw new RuntimeException("Cannot deactivate category: " + productCount
                    + " active product(s) belong to this category. Reassign or deactivate them first.");
        }

        category.setActive(false);
        categoryRepository.save(category);
    }

    private CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .genderType(category.getGenderType() != null ? category.getGenderType().name() : null)
                .displayOrder(category.getDisplayOrder())
                .metaTitle(category.getMetaTitle())
                .metaDescription(category.getMetaDescription())
                .metaKeywords(category.getMetaKeywords())
                .ogImage(category.getOgImage())
                .build();
    }

    private CategoryDTO toDTOWithSubcategories(Category category) {
        CategoryDTO dto = toDTO(category);
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            dto.setSubcategories(
                    category.getSubcategories().stream()
                            .filter(Category::isActive)
                            .map(this::toDTO)
                            .collect(Collectors.toList())
            );
        }
        return dto;
    }
}
