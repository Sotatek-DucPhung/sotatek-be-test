package com.sotatek.order.service.external.adapter;

import com.sotatek.order.service.external.PaymentServiceClient;
import com.sotatek.order.service.external.dto.PaymentDto;
import com.sotatek.order.service.external.dto.PaymentRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock implementation of Payment Service Client
 * Simulates payment processing for testing and development
 * Use @Primary to make this the default implementation
 */
@Component
@ConditionalOnProperty(name = "external.mock.enabled", havingValue = "true", matchIfMissing = true)
@Primary
@Slf4j
public class MockPaymentServiceClient implements PaymentServiceClient {

    private final AtomicLong paymentIdGenerator = new AtomicLong(5000L);

    @Override
    public PaymentDto createPayment(PaymentRequestDto request) {
        log.info("[MOCK] Creating payment: orderId={}, amount={}, method={}",
                request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        // Simulate payment failure for orderId 6666
        if (request.getOrderId() == 6666L) {
            log.warn("[MOCK] Payment FAILED for orderId={}", request.getOrderId());
            throw new RuntimeException("Payment failed: Insufficient funds");
        }

        // Generate mock successful payment
        Long paymentId = paymentIdGenerator.incrementAndGet();
        String transactionId = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentDto payment = PaymentDto.builder()
                .id(paymentId)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status("COMPLETED")
                .transactionId(transactionId)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        log.info("[MOCK] Payment created successfully: paymentId={}, transactionId={}", paymentId, transactionId);
        return payment;
    }

    @Override
    public PaymentDto getPayment(Long paymentId) {
        log.info("[MOCK] Getting payment: paymentId={}", paymentId);

        if (paymentId == 9999L) {
            log.warn("[MOCK] Payment not found: paymentId={}", paymentId);
            throw new RuntimeException("Payment not found: paymentId=" + paymentId);
        }

        // Return mock payment
        return PaymentDto.builder()
                .id(paymentId)
                .orderId(1L)
                .amount(java.math.BigDecimal.valueOf(99.99))
                .status("COMPLETED")
                .transactionId("TXN-MOCK-" + paymentId)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
