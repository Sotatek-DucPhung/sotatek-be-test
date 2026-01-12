package com.sotatek.order.service.external;

import com.sotatek.order.service.external.dto.PaymentDto;
import com.sotatek.order.service.external.dto.PaymentRequestDto;

/**
 * Adapter interface for Payment Service
 * Allows easy swapping between mock and real implementations
 */
public interface PaymentServiceClient {

    /**
     * Create a payment in Payment Service
     *
     * @param request the payment request
     * @return the payment DTO
     * @throws RuntimeException if payment fails or service unavailable
     */
    PaymentDto createPayment(PaymentRequestDto request);

    /**
     * Get payment by ID from Payment Service
     *
     * @param paymentId the payment ID
     * @return the payment DTO
     * @throws RuntimeException if payment not found or service unavailable
     */
    PaymentDto getPayment(Long paymentId);
}
