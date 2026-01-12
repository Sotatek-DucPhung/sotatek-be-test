package com.sotatek.order.exception;

public class InsufficientStockException extends OrderException {

    public InsufficientStockException(String message) {
        super("INSUFFICIENT_STOCK", message);
    }
}
