package com.kalon.dto;

import com.kalon.entity.ReturnRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReturnRequestDTO {
    @NotNull(message = "Order item ID is required")
    private Long orderItemId;

    @NotNull(message = "Request type is required (RETURN or EXCHANGE)")
    private ReturnRequest.RequestType requestType;

    @NotNull(message = "Reason is required")
    private String reason;

    private Long exchangeProductId;
    private Long exchangeVariantId;
}
