package com.sotatek.order.exception;

public class PaymentNotFoundException extends OrderException {

    public PaymentNotFoundException(Long paymentId) {
        super("PAYMENT_NOT_FOUND", "Payment not found: id=" + paymentId);
    }
}
