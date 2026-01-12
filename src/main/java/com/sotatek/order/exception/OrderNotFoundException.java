package com.sotatek.order.exception;

public class OrderNotFoundException extends OrderException {

    public OrderNotFoundException(Long orderId) {
        super("ORDER_NOT_FOUND", "Order not found: id=" + orderId);
    }
}
