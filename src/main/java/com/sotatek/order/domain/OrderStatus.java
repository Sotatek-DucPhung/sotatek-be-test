package com.sotatek.order.domain;

/**
 * Order status enum representing the lifecycle of an order.
 *
 * State transitions:
 * PENDING → CONFIRMED
 * CONFIRMED → CANCELLED (user cancellation)
 */
public enum OrderStatus {
    /**
     * Order created, before payment
     */
    PENDING,

    /**
     * Payment completed, order confirmed
     */
    CONFIRMED,

    /**
     * Order cancelled by user
     */
    CANCELLED
}
