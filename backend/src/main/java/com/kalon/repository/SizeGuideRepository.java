package com.kalon.repository;

import com.kalon.entity.Category;
import com.kalon.entity.SizeGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SizeGuideRepository extends JpaRepository<SizeGuide, Long> {
    List<SizeGuide> findByIsActiveTrue();
    List<SizeGuide> findByCategoryIdAndIsActiveTrue(Long categoryId);
    List<SizeGuide> findByGenderTypeAndIsActiveTrue(Category.GenderType genderType);
    Optional<SizeGuide> findByCategoryIdAndGenderTypeAndIsActiveTrue(Long categoryId, Category.GenderType genderType);
}
