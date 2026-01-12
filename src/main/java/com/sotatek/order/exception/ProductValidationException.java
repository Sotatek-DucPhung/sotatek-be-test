package com.sotatek.order.exception;

public class ProductValidationException extends OrderException {

    public ProductValidationException(String message) {
        super("PRODUCT_VALIDATION_ERROR", message);
    }
}
