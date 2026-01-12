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
     * @throws com.sotatek.order.exception.ProductNotFoundException if product not found
     * @throws com.sotatek.order.exception.ExternalServiceException if service unavailable
     */
    ProductDto getProduct(Long productId);

    /**
     * Get product stock information from Product Service
     *
     * @param productId the product ID
     * @return the product stock DTO
     * @throws com.sotatek.order.exception.ProductNotFoundException if product not found
     * @throws com.sotatek.order.exception.ExternalServiceException if service unavailable
     */
    ProductStockDto getProductStock(Long productId);
}
