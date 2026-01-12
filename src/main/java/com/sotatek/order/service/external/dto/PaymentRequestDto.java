package com.sotatek.order.service.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Payment Request to external Payment Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    private Long orderId;
    private BigDecimal amount;
    private String paymentMethod;  // CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER
}
