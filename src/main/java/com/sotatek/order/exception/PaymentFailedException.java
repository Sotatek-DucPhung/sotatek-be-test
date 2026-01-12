package com.sotatek.order.exception;

public class PaymentFailedException extends OrderException {

    public PaymentFailedException(String message) {
        super("PAYMENT_FAILED", message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super("PAYMENT_FAILED", message, cause);
    }
}
