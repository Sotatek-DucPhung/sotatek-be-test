package com.sotatek.order.repository;

import com.sotatek.order.domain.Order;
import com.sotatek.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Order entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find an order by ID with its items fetched eagerly to avoid N+1 problem
     *
     * @param id the order ID
     * @return Optional containing the order with items, or empty if not found
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    /**
     * Find all orders by member ID with pagination
     *
     * @param memberId the member ID
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByMemberId(Long memberId, Pageable pageable);

    /**
     * Find all orders by status with pagination
     *
     * @param status the order status
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find all orders by member ID and status with pagination
     *
     * @param memberId the member ID
     * @param status the order status
     * @param pageable pagination information
     * @return page of orders
     */
    Page<Order> findByMemberIdAndStatus(Long memberId, OrderStatus status, Pageable pageable);

    /**
     * Count orders by member ID
     *
     * @param memberId the member ID
     * @return count of orders
     */
    long countByMemberId(Long memberId);

    /**
     * Count orders by status
     *
     * @param status the order status
     * @return count of orders
     */
    long countByStatus(OrderStatus status);
}
