package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfigDTO {
    private Long id;
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private boolean isActive;
    private Integer displayOrder;
}



