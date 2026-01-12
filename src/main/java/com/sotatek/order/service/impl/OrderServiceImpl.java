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
import com.sotatek.order.exception.InsufficientStockException;
import com.sotatek.order.exception.InvalidOrderStatusException;
import com.sotatek.order.exception.MemberValidationException;
import com.sotatek.order.exception.OrderNotFoundException;
import com.sotatek.order.exception.PaymentFailedException;
import com.sotatek.order.exception.ProductValidationException;
import com.sotatek.order.repository.OrderRepository;
import com.sotatek.order.service.OrderService;
import com.sotatek.order.service.external.MemberServiceClient;
import com.sotatek.order.service.external.PaymentServiceClient;
import com.sotatek.order.service.external.ProductServiceClient;
import com.sotatek.order.service.external.dto.*;
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
 * Handles core order business logic with external service integration
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MemberServiceClient memberServiceClient;
    private final ProductServiceClient productServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for member: {}", request.getMemberId());

        // Step 1: Validate member exists and is active
        log.debug("Validating member: memberId={}", request.getMemberId());
        MemberDto member = memberServiceClient.getMember(request.getMemberId());

        if (!"ACTIVE".equals(member.getStatus())) {
            log.error("Member is not active: memberId={}, status={}", request.getMemberId(), member.getStatus());
            throw new MemberValidationException("Member is not active: status=" + member.getStatus());
        }

        // Create order entity with validated member info
        Order order = Order.builder()
                .memberId(request.getMemberId())
                .memberName(member.getName())
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Step 2: Validate products and add order items
        for (OrderItemRequest itemRequest : request.getItems()) {
            Long productId = itemRequest.getProductId();
            Integer requestedQuantity = itemRequest.getQuantity();

            log.debug("Validating product: productId={}, requestedQuantity={}", productId, requestedQuantity);

            // Validate product exists and is available
            ProductDto product = productServiceClient.getProduct(productId);

            if (!"AVAILABLE".equals(product.getStatus())) {
                log.error("Product is not available: productId={}, status={}", productId, product.getStatus());
                throw new ProductValidationException("Product is not available: productId=" + productId +
                        ", status=" + product.getStatus());
            }

            // Check stock availability
            ProductStockDto stock = productServiceClient.getProductStock(productId);

            if (stock.getAvailableQuantity() < requestedQuantity) {
                log.error("Insufficient stock: productId={}, requested={}, available={}",
                        productId, requestedQuantity, stock.getAvailableQuantity());
                throw new InsufficientStockException("Insufficient stock for product: productId=" + productId +
                        ", requested=" + requestedQuantity + ", available=" + stock.getAvailableQuantity());
            }

            // Create order item with real product data
            OrderItem item = OrderItem.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(requestedQuantity)
                    .build();

            item.calculateSubtotal();
            order.addItem(item);
        }

        // Calculate total amount
        order.calculateTotalAmount();

        // Save order with PENDING status
        order = orderRepository.save(order);
        log.info("Order saved with PENDING status: orderId={}, totalAmount={}",
                order.getId(), order.getTotalAmount());

        // Step 3: Process payment
        try {
            log.info("Processing payment: orderId={}, amount={}, method={}",
                    order.getId(), order.getTotalAmount(), order.getPaymentMethod());

            PaymentRequestDto paymentRequest = PaymentRequestDto.builder()
                    .orderId(order.getId())
                    .amount(order.getTotalAmount())
                    .paymentMethod(order.getPaymentMethod().name())
                    .build();

            PaymentDto payment = paymentServiceClient.createPayment(paymentRequest);

            // Update order with payment details and confirm
            order.setPaymentId(payment.getId());
            order.setTransactionId(payment.getTransactionId());
            order.setStatus(OrderStatus.CONFIRMED);

            order = orderRepository.save(order);

            log.info("Payment processed successfully: orderId={}, paymentId={}, transactionId={}",
                    order.getId(), payment.getId(), payment.getTransactionId());

        } catch (PaymentFailedException e) {
            log.error("Payment failed for orderId={}: {}", order.getId(), e.getMessage());
            // Order remains in PENDING status, payment can be retried
            throw e;
        } catch (Exception e) {
            log.error("Payment failed for orderId={}: {}", order.getId(), e.getMessage());
            // Order remains in PENDING status, payment can be retried
            throw new PaymentFailedException("Payment processing failed: " + e.getMessage(), e);
        }

        log.info("Order created successfully: orderId={}, status={}", order.getId(), order.getStatus());

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> {
                    log.error("Order not found: id={}", id);
                    throw new OrderNotFoundException(id);
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
                    throw new OrderNotFoundException(id);
                });

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            log.error("Order update rejected: items update is not allowed");
            throw new InvalidOrderStatusException("Only status update is allowed. Items cannot be updated.");
        }

        if (request.getPaymentMethod() != null) {
            log.error("Order update rejected: payment method update is not allowed");
            throw new InvalidOrderStatusException("Only status update is allowed. Payment method cannot be updated.");
        }

        if (request.getStatus() == null) {
            log.error("Order update rejected: status is required");
            throw new InvalidOrderStatusException("Status is required for order update.");
        }

        if (request.getStatus() != OrderStatus.CANCELLED) {
            log.error("Order update rejected: only CANCELLED status is supported, requested={}", request.getStatus());
            throw new InvalidOrderStatusException("Only status change to CANCELLED is supported.");
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            log.error("Order update rejected: only CONFIRMED orders can be cancelled, status={}", order.getStatus());
            throw new InvalidOrderStatusException("Only CONFIRMED orders can be cancelled.");
        }

        log.info("Updating order status: {} → {}", order.getStatus(), request.getStatus());
        order.setStatus(OrderStatus.CANCELLED);

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
