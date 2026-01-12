package com.sotatek.order.service.external.adapter;

import com.sotatek.order.service.external.ProductServiceClient;
import com.sotatek.order.service.external.dto.ProductDto;
import com.sotatek.order.service.external.dto.ProductStockDto;
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
public class RestProductServiceClient implements ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${external.product-service.url}")
    private String baseUrl;

    @Override
    @CircuitBreaker(name = "productService")
    @Retry(name = "productService")
    public ProductDto getProduct(Long productId) {
        String url = baseUrl + "/api/products/" + productId;

        try {
            ProductDto product = restTemplate.getForObject(url, ProductDto.class);
            if (product == null) {
                throw new RuntimeException("Product service returned empty response: productId=" + productId);
            }
            return product;
        } catch (RestClientResponseException ex) {
            log.error("Product service error: status={}, body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Product service error: status=" + ex.getRawStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("Product service call failed: {}", ex.getMessage());
            throw new RuntimeException("Product service call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    @CircuitBreaker(name = "productService")
    @Retry(name = "productService")
    public ProductStockDto getProductStock(Long productId) {
        String url = baseUrl + "/api/products/" + productId + "/stock";

        try {
            ProductStockDto stock = restTemplate.getForObject(url, ProductStockDto.class);
            if (stock == null) {
                throw new RuntimeException("Product stock response is empty: productId=" + productId);
            }
            return stock;
        } catch (RestClientResponseException ex) {
            log.error("Product stock error: status={}, body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Product stock error: status=" + ex.getRawStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("Product stock call failed: {}", ex.getMessage());
            throw new RuntimeException("Product stock call failed: " + ex.getMessage(), ex);
        }
    }
}
