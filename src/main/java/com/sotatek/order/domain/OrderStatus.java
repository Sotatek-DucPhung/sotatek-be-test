package com.sotatek.order.domain;

/**
 * Order status enum representing the lifecycle of an order.
 *
 * State transitions:
 * PENDING → CONFIRMED → PAID → COMPLETED
 *    ↓           ↓
 * CANCELLED   PAYMENT_FAILED
 */
public enum OrderStatus {
    /**
     * Order created, validation in progress
     */
    PENDING,

    /**
     * Validation passed (member, product, stock), ready for payment
     */
    CONFIRMED,

    /**
     * Payment successful
     */
    PAID,

    /**
     * Order fulfilled (future state)
     */
    COMPLETED,

    /**
     * Order cancelled by user
     */
    CANCELLED,

    /**
     * Payment processing failed
     */
    PAYMENT_FAILED
}
