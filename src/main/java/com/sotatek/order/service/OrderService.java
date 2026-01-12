package com.sotatek.order.service;

import com.sotatek.order.controller.request.CreateOrderRequest;
import com.sotatek.order.controller.request.UpdateOrderRequest;
import com.sotatek.order.controller.response.OrderResponse;
import com.sotatek.order.controller.response.PageResponse;
import com.sotatek.order.domain.OrderStatus;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for order business logic
 */
public interface OrderService {

    /**
     * Create a new order
     *
     * @param request the order creation request
     * @return the created order response
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Get an order by ID
     *
     * @param id the order ID
     * @return the order response
     */
    OrderResponse getOrderById(Long id);

    /**
     * List orders with optional filtering and pagination
     *
     * @param memberId optional member ID filter
     * @param status optional order status filter
     * @param pageable pagination information
     * @return paginated list of orders
     */
    PageResponse<OrderResponse> listOrders(Long memberId, OrderStatus status, Pageable pageable);

    /**
     * Update an existing order
     * - Can update items and payment method for PENDING orders (before payment)
     * - Can change status from CONFIRMED to CANCELLED (user cancellation)
     *
     * @param id the order ID
     * @param request the order update request
     * @return the updated order response
     */
    OrderResponse updateOrder(Long id, UpdateOrderRequest request);
}
