package com.sotatek.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity representing a customer order
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_member_id", columnList = "member_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "member_name", nullable = false)
    private String memberName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback - executed before persisting the entity
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    /**
     * JPA lifecycle callback - executed before updating the entity
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Add an item to the order and establish bidirectional relationship
     *
     * @param item the order item to add
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Remove an item from the order
     *
     * @param item the order item to remove
     */
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    /**
     * Calculate and update the total amount based on all order items
     */
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if the order items can be updated
     * Only PENDING orders (before payment) can update items
     *
     * @return true if the order items can be updated, false otherwise
     */
    public boolean canUpdateItems() {
        return status == OrderStatus.PENDING;
    }

    /**
     * Check if the order status can be changed to CANCELLED
     * Only CONFIRMED orders can be cancelled by user
     *
     * @return true if the order can be cancelled, false otherwise
     */
    public boolean canBeCancelled() {
        return status == OrderStatus.CONFIRMED;
    }

    /**
     * Check if the status transition is valid
     *
     * @param newStatus the new status to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        if (status == newStatus) {
            return true; // No change
        }

        return switch (status) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED;
            case CONFIRMED -> newStatus == OrderStatus.CANCELLED;
            case CANCELLED -> false; // Terminal state
        };
    }

    /**
     * Clear all items from the order
     */
    public void clearItems() {
        items.forEach(item -> item.setOrder(null));
        items.clear();
    }
}
