package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Long parentId;
    private String parentName;
    private String genderType;
    private Integer displayOrder;
    private List<CategoryDTO> subcategories;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String ogImage;
}
