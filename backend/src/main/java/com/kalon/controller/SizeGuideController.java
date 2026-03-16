package com.kalon.controller;

import com.kalon.dto.ApiResponse;
import com.kalon.dto.SizeGuideDTO;
import com.kalon.entity.Category;
import com.kalon.entity.SizeGuide;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.SizeGuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/size-guides")
@RequiredArgsConstructor
public class SizeGuideController {

    private final SizeGuideRepository sizeGuideRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SizeGuideDTO>>> getAllSizeGuides() {
        List<SizeGuideDTO> guides = sizeGuideRepository.findByIsActiveTrue()
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Size guides", guides));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SizeGuideDTO>> getSizeGuideById(@PathVariable Long id) {
        SizeGuide guide = sizeGuideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Size guide not found"));
        return ResponseEntity.ok(ApiResponse.success("Size guide", toDTO(guide)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<SizeGuideDTO>>> getSizeGuidesByCategory(@PathVariable Long categoryId) {
        List<SizeGuideDTO> guides = sizeGuideRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Size guides for category", guides));
    }

    @GetMapping("/gender/{gender}")
    public ResponseEntity<ApiResponse<List<SizeGuideDTO>>> getSizeGuidesByGender(@PathVariable String gender) {
        Category.GenderType genderType = Category.GenderType.valueOf(gender.toUpperCase());
        List<SizeGuideDTO> guides = sizeGuideRepository.findByGenderTypeAndIsActiveTrue(genderType)
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Size guides for gender", guides));
    }

    private SizeGuideDTO toDTO(SizeGuide guide) {
        return SizeGuideDTO.builder()
                .id(guide.getId())
                .name(guide.getName())
                .categoryId(guide.getCategory() != null ? guide.getCategory().getId() : null)
                .categoryName(guide.getCategory() != null ? guide.getCategory().getName() : null)
                .genderType(guide.getGenderType() != null ? guide.getGenderType().name() : null)
                .measurements(guide.getMeasurements())
                .sizeChartImage(guide.getSizeChartImage())
                .fitNotes(guide.getFitNotes())
                .isActive(guide.isActive())
                .build();
    }
}
