package com.sotatek.order.service.impl;

import com.sotatek.order.controller.request.CreateOrderRequest;
import com.sotatek.order.controller.request.OrderItemRequest;
import com.sotatek.order.controller.request.UpdateOrderRequest;
import com.sotatek.order.controller.response.OrderItemResponse;
import com.sotatek.order.controller.response.OrderResponse;
import com.sotatek.order.controller.response.PageResponse;
import com.sotatek.order.domain.Order;
import com.sotatek.order.domain.OrderItem;
import com.sotatek.order.domain.OrderStatus;
import com.sotatek.order.repository.OrderRepository;
import com.sotatek.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of OrderService
 * Handles core order business logic
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for member: {}", request.getMemberId());

        // For now, create order without external service validation
        // Phase 3 will add member/product/payment validation

        // Create order entity
        Order order = Order.builder()
                .memberId(request.getMemberId())
                .memberName("Member " + request.getMemberId()) // Placeholder, will be from Member Service
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Add order items
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName("Product " + itemRequest.getProductId()) // Placeholder
                    .unitPrice(BigDecimal.valueOf(99.99)) // Placeholder price
                    .quantity(itemRequest.getQuantity())
                    .build();

            item.calculateSubtotal();
            order.addItem(item);
        }

        // Calculate total amount
        order.calculateTotalAmount();

        // Save order
        order = orderRepository.save(order);

        log.info("Order created successfully: orderId={}", order.getId());

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> {
                    log.error("Order not found: id={}", id);
                    throw new RuntimeException("Order not found: id=" + id);
                });

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listOrders(Long memberId, OrderStatus status, Pageable pageable) {
        log.debug("Listing orders: memberId={}, status={}, page={}", memberId, status, pageable.getPageNumber());

        Page<Order> orderPage;

        if (memberId != null && status != null) {
            orderPage = orderRepository.findByMemberIdAndStatus(memberId, status, pageable);
        } else if (memberId != null) {
            orderPage = orderRepository.findByMemberId(memberId, pageable);
        } else if (status != null) {
            orderPage = orderRepository.findByStatus(status, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        List<OrderResponse> content = orderPage.getContent().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());

        PageResponse.PageInfo pageInfo = PageResponse.PageInfo.builder()
                .number(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .build();

        return PageResponse.<OrderResponse>builder()
                .content(content)
                .page(pageInfo)
                .build();
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        log.info("Updating order: id={}", id);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> {
                    log.error("Order not found: id={}", id);
                    throw new RuntimeException("Order not found: id=" + id);
                });

        // Handle status update (e.g., CONFIRMED → CANCELLED)
        if (request.getStatus() != null && request.getStatus() != order.getStatus()) {
            if (!order.canTransitionTo(request.getStatus())) {
                log.error("Invalid status transition: {} → {}", order.getStatus(), request.getStatus());
                throw new RuntimeException("Cannot change order status from " + order.getStatus() +
                        " to " + request.getStatus());
            }
            log.info("Updating order status: {} → {}", order.getStatus(), request.getStatus());
            order.setStatus(request.getStatus());
        }

        // Handle items update (only for PENDING orders)
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            if (!order.canUpdateItems()) {
                log.error("Cannot update items for order with status: {}", order.getStatus());
                throw new RuntimeException("Cannot update order items with status: " + order.getStatus() +
                        ". Only PENDING orders can update items.");
            }

            // Clear existing items
            order.clearItems();

            // Add new items
            for (OrderItemRequest itemRequest : request.getItems()) {
                OrderItem item = OrderItem.builder()
                        .productId(itemRequest.getProductId())
                        .productName("Product " + itemRequest.getProductId()) // Placeholder
                        .unitPrice(BigDecimal.valueOf(99.99)) // Placeholder price
                        .quantity(itemRequest.getQuantity())
                        .build();

                item.calculateSubtotal();
                order.addItem(item);
            }

            // Recalculate total amount
            order.calculateTotalAmount();
        }

        // Update payment method if provided
        if (request.getPaymentMethod() != null) {
            order.setPaymentMethod(request.getPaymentMethod());
        }

        // Save updated order
        order = orderRepository.save(order);

        log.info("Order updated successfully: orderId={}", order.getId());

        return mapToOrderResponse(order);
    }

    /**
     * Map Order entity to OrderResponse DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .memberId(order.getMemberId())
                .memberName(order.getMemberName())
                .status(order.getStatus())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentId(order.getPaymentId())
                .transactionId(order.getTransactionId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
