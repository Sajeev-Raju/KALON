package com.kalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCustomerDTO {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private Long orderCount;
    private BigDecimal totalSpent;
}
