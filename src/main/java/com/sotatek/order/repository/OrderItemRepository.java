package com.sotatek.order.repository;

import com.sotatek.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for OrderItem entity
 * Provides CRUD operations and custom queries
 * Note: Most OrderItem operations are handled through cascade from Order entity
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all order items by order ID
     *
     * @param orderId the order ID
     * @return list of order items
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Find all order items by product ID
     * Useful for analyzing which orders contain a specific product
     *
     * @param productId the product ID
     * @return list of order items
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * Count order items by order ID
     *
     * @param orderId the order ID
     * @return count of items in the order
     */
    long countByOrderId(Long orderId);

    /**
     * Delete all order items by order ID
     *
     * @param orderId the order ID
     */
    void deleteByOrderId(Long orderId);

    /**
     * Check if any order items exist for a specific product
     *
     * @param productId the product ID
     * @return true if items exist, false otherwise
     */
    boolean existsByProductId(Long productId);

    /**
     * Get total quantity ordered for a specific product
     *
     * @param productId the product ID
     * @return total quantity across all orders
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productId = :productId")
    Long getTotalQuantityByProductId(@Param("productId") Long productId);
}
