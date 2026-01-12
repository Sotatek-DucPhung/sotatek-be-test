package com.sotatek.order.service.external.adapter;

import com.sotatek.order.service.external.ProductServiceClient;
import com.sotatek.order.service.external.dto.ProductDto;
import com.sotatek.order.service.external.dto.ProductStockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mock implementation of Product Service Client
 * Returns hardcoded product data for testing and development
 * Use @Primary to make this the default implementation
 */
@Component
@ConditionalOnProperty(name = "external.mock.enabled", havingValue = "true", matchIfMissing = true)
@Primary
@Slf4j
public class MockProductServiceClient implements ProductServiceClient {

    @Override
    public ProductDto getProduct(Long productId) {
        log.info("[MOCK] Getting product: productId={}", productId);

        // Simulate some products not found or unavailable
        if (productId == 9999L) {
            log.warn("[MOCK] Product not found: productId={}", productId);
            throw new RuntimeException("Product not found: productId=" + productId);
        }

        if (productId == 8888L) {
            log.warn("[MOCK] Product is OUT_OF_STOCK: productId={}", productId);
            return ProductDto.builder()
                    .id(productId)
                    .name("Out of Stock Product")
                    .price(BigDecimal.valueOf(0.00))
                    .status("OUT_OF_STOCK")
                    .build();
        }

        // Return mock available product for all other IDs
        log.info("[MOCK] Returning mock product: productId={}, status=AVAILABLE", productId);
        return ProductDto.builder()
                .id(productId)
                .name("Mock Product " + productId)
                .price(BigDecimal.valueOf(99.99))
                .status("AVAILABLE")
                .build();
    }

    @Override
    public ProductStockDto getProductStock(Long productId) {
        log.info("[MOCK] Getting product stock: productId={}", productId);

        // Simulate product 7777 having insufficient stock
        if (productId == 7777L) {
            log.warn("[MOCK] Insufficient stock: productId={}", productId);
            return ProductStockDto.builder()
                    .productId(productId)
                    .quantity(10)
                    .reservedQuantity(8)
                    .availableQuantity(2)  // Only 2 available
                    .build();
        }

        // Return mock stock with plenty available
        log.info("[MOCK] Returning mock stock: productId={}, available=1000", productId);
        return ProductStockDto.builder()
                .productId(productId)
                .quantity(1000)
                .reservedQuantity(0)
                .availableQuantity(1000)
                .build();
    }
}
