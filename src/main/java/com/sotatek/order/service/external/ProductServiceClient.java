package com.sotatek.order.service.external;

import com.sotatek.order.service.external.dto.ProductDto;
import com.sotatek.order.service.external.dto.ProductStockDto;

/**
 * Adapter interface for Product Service
 * Allows easy swapping between mock and real implementations
 */
public interface ProductServiceClient {

    /**
     * Get product by ID from Product Service
     *
     * @param productId the product ID
     * @return the product DTO
     * @throws RuntimeException if product not found or service unavailable
     */
    ProductDto getProduct(Long productId);

    /**
     * Get product stock information from Product Service
     *
     * @param productId the product ID
     * @return the product stock DTO
     * @throws RuntimeException if product not found or service unavailable
     */
    ProductStockDto getProductStock(Long productId);
}
