package com.sotatek.order.service.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for Payment from external Payment Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {

    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String status;  // PENDING, COMPLETED, FAILED, REFUNDED
    private String transactionId;
    private OffsetDateTime createdAt;
}
