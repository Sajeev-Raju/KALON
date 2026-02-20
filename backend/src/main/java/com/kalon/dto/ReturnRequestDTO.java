package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private OrderItemDTO orderItem;
    private String requestType;
    private String status;
    private String reason;
    private BigDecimal refundAmount;
    private String refundId;
    private String refundStatus;
    private Long exchangeProductId;
    private Long exchangeVariantId;
    private String exchangeProductName;
    private String exchangeVariantInfo;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
