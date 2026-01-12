package com.sotatek.order.exception;

public class InvalidOrderStatusException extends OrderException {

    public InvalidOrderStatusException(String message) {
        super("INVALID_ORDER_STATUS", message);
    }
}
