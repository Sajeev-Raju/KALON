package com.kalon.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SizeGuideDTO {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String genderType;
    private String measurements;
    private String sizeChartImage;
    private String fitNotes;
    private boolean isActive;
}
