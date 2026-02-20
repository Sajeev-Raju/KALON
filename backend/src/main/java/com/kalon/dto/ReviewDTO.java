package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private Long userId;
    private String userName;
    private Integer rating;
    private String comment;
    private boolean isVerifiedPurchase;
    private boolean isApproved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
