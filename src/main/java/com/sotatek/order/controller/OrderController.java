package com.sotatek.order.controller;

import com.sotatek.order.controller.request.CreateOrderRequest;
import com.sotatek.order.controller.request.UpdateOrderRequest;
import com.sotatek.order.controller.response.OrderResponse;
import com.sotatek.order.controller.response.PageResponse;
import com.sotatek.order.domain.OrderStatus;
import com.sotatek.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order management
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with validation and payment processing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or business rule violation"),
            @ApiResponse(responseCode = "404", description = "Member or product not found"),
            @ApiResponse(responseCode = "422", description = "Payment processing failed"),
            @ApiResponse(responseCode = "503", description = "External service unavailable")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("Received create order request: memberId={}", request.getMemberId());

        OrderResponse response = orderService.createOrder(request);

        log.info("Order created successfully: orderId={}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves a specific order by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id) {
        log.info("Received get order request: orderId={}", id);

        OrderResponse response = orderService.getOrderById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Retrieves a paginated list of orders with optional filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    public ResponseEntity<PageResponse<OrderResponse>> listOrders(
            @Parameter(description = "Filter by member ID")
            @RequestParam(required = false) Long memberId,
            @Parameter(description = "Filter by order status")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g., createdAt,desc)")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        log.info("Received list orders request: memberId={}, status={}, page={}, size={}",
                memberId, status, page, size);

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        PageResponse<OrderResponse> response = orderService.listOrders(memberId, status, pageable);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order",
               description = "Updates an existing order. Can update items/payment method for PENDING orders (before payment). Can change status from CONFIRMED to CANCELLED (user cancellation).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> updateOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request) {
        log.info("Received update order request: orderId={}", id);

        OrderResponse response = orderService.updateOrder(id, request);

        log.info("Order updated successfully: orderId={}", response.getId());

        return ResponseEntity.ok(response);
    }
}
