package com.kalon.repository;

import com.kalon.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    List<Category> findByParentIsNullAndIsActiveTrue();
    List<Category> findByParentIdAndIsActiveTrue(Long parentId);
    List<Category> findByGenderTypeAndIsActiveTrue(Category.GenderType genderType);

    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.displayOrder ASC")
    List<Category> findAllActiveOrderByDisplayOrder();

    @Query("SELECT DISTINCT c.name FROM Category c WHERE c.isActive = true AND " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.name")
    List<String> findCategoryNameSuggestions(@Param("keyword") String keyword, Pageable pageable);
}
