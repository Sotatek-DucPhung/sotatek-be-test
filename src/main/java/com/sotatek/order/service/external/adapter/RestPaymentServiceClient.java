package com.sotatek.order.service.external.adapter;

import com.sotatek.order.service.external.PaymentServiceClient;
import com.sotatek.order.service.external.dto.PaymentDto;
import com.sotatek.order.service.external.dto.PaymentRequestDto;
import com.sotatek.order.exception.ExternalServiceException;
import com.sotatek.order.exception.PaymentFailedException;
import com.sotatek.order.exception.PaymentNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "external.mock.enabled", havingValue = "false")
@Slf4j
@RequiredArgsConstructor
public class RestPaymentServiceClient implements PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${external.payment-service.url}")
    private String baseUrl;

    @Override
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService")
    public PaymentDto createPayment(PaymentRequestDto request) {
        String url = baseUrl + "/api/payments";

        try {
            PaymentDto payment = restTemplate.postForObject(url, request, PaymentDto.class);
            if (payment == null) {
                throw new ExternalServiceException("Payment service returned empty response: orderId=" + request.getOrderId());
            }
            return payment;
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() == 400 || ex.getRawStatusCode() == 422) {
                throw new PaymentFailedException("Payment request rejected: status=" + ex.getRawStatusCode(), ex);
            }
            log.error("Payment service error: status={}, body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new ExternalServiceException("Payment service error: status=" + ex.getRawStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("Payment service call failed: {}", ex.getMessage());
            throw new ExternalServiceException("Payment service call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService")
    public PaymentDto getPayment(Long paymentId) {
        String url = baseUrl + "/api/payments/" + paymentId;

        try {
            PaymentDto payment = restTemplate.getForObject(url, PaymentDto.class);
            if (payment == null) {
                throw new ExternalServiceException("Payment service returned empty response: paymentId=" + paymentId);
            }
            return payment;
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() == 404) {
                throw new PaymentNotFoundException(paymentId);
            }
            log.error("Payment service error: status={}, body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new ExternalServiceException("Payment service error: status=" + ex.getRawStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("Payment service call failed: {}", ex.getMessage());
            throw new ExternalServiceException("Payment service call failed: " + ex.getMessage(), ex);
        }
    }
}
