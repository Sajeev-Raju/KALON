package com.kalon.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminActivityLogDTO {
    private Long id;
    private Long adminId;
    private String adminName;
    private String adminEmail;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    private String details;
    private LocalDateTime createdAt;
}
