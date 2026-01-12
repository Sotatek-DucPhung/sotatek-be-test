package com.sotatek.order.exception;

public class ProductNotFoundException extends OrderException {

    public ProductNotFoundException(Long productId) {
        super("PRODUCT_NOT_FOUND", "Product not found: id=" + productId);
    }
}
