package com.sotatek.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sotatek.order.controller.request.CreateOrderRequest;
import com.sotatek.order.controller.request.OrderItemRequest;
import com.sotatek.order.controller.request.UpdateOrderRequest;
import com.sotatek.order.controller.response.OrderItemResponse;
import com.sotatek.order.controller.response.OrderResponse;
import com.sotatek.order.controller.response.PageResponse;
import com.sotatek.order.domain.OrderStatus;
import com.sotatek.order.domain.PaymentMethod;
import com.sotatek.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for happy path scenarios
 * Tests the REST API layer with successful requests/responses
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrderReturns201Created() throws Exception {
        // Arrange
        CreateOrderRequest request = CreateOrderRequest.builder()
                .memberId(1L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder()
                        .productId(2001L)
                        .quantity(2)
                        .build()))
                .build();

        OrderResponse response = buildOrderResponse(1L, OrderStatus.CONFIRMED);

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.memberName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value(2001))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(29.99))
                .andExpect(jsonPath("$.totalAmount").value(59.98))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.paymentId").value(100))
                .andExpect(jsonPath("$.transactionId").value("TXN-12345"));
    }

    @Test
    void getOrderByIdReturns200Ok() throws Exception {
        // Arrange
        OrderResponse response = buildOrderResponse(1L, OrderStatus.CONFIRMED);

        when(orderService.getOrderById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.memberName").value("John Doe"));
    }

    @Test
    void listOrdersReturns200OkWithPagination() throws Exception {
        // Arrange
        List<OrderResponse> orders = List.of(
                buildOrderResponse(1L, OrderStatus.CONFIRMED),
                buildOrderResponse(2L, OrderStatus.PENDING)
        );

        PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                .content(orders)
                .page(PageResponse.PageInfo.builder()
                        .number(0)
                        .size(10)
                        .totalElements(2L)
                        .totalPages(1)
                        .build())
                .build();

        when(orderService.listOrders(eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void listOrdersFilteredByMemberId() throws Exception {
        // Arrange
        List<OrderResponse> orders = List.of(buildOrderResponse(1L, OrderStatus.CONFIRMED));

        PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                .content(orders)
                .page(PageResponse.PageInfo.builder()
                        .number(0)
                        .size(10)
                        .totalElements(1L)
                        .totalPages(1)
                        .build())
                .build();

        when(orderService.listOrders(eq(1L), eq(null), any(PageRequest.class)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("memberId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].memberId").value(1));
    }

    @Test
    void listOrdersFilteredByStatus() throws Exception {
        // Arrange
        List<OrderResponse> orders = List.of(buildOrderResponse(1L, OrderStatus.CONFIRMED));

        PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                .content(orders)
                .page(PageResponse.PageInfo.builder()
                        .number(0)
                        .size(10)
                        .totalElements(1L)
                        .totalPages(1)
                        .build())
                .build();

        when(orderService.listOrders(eq(null), eq(OrderStatus.CONFIRMED), any(PageRequest.class)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("status", "CONFIRMED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    void updateOrderReturns200Ok() throws Exception {
        // Arrange
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .items(List.of(OrderItemRequest.builder()
                        .productId(2001L)
                        .quantity(3)
                        .build()))
                .build();

        OrderResponse response = buildOrderResponse(1L, OrderStatus.PENDING);

        when(orderService.updateOrder(eq(1L), any(UpdateOrderRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void updateOrderCancelsConfirmedOrder() throws Exception {
        // Arrange
        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        OrderResponse response = buildOrderResponse(1L, OrderStatus.CANCELLED);

        when(orderService.updateOrder(eq(1L), any(UpdateOrderRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void createOrderValidatesRequiredFields() throws Exception {
        // Arrange - missing memberId and items
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createOrderValidatesItemQuantity() throws Exception {
        // Arrange - quantity must be at least 1
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .memberId(1L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder()
                        .productId(2001L)
                        .quantity(0) // Invalid
                        .build()))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // Helper method to build order response
    private OrderResponse buildOrderResponse(Long id, OrderStatus status) {
        return OrderResponse.builder()
                .id(id)
                .memberId(1L)
                .memberName("John Doe")
                .status(status)
                .items(List.of(
                        OrderItemResponse.builder()
                                .id(1L)
                                .productId(2001L)
                                .productName("Wireless Mouse")
                                .unitPrice(BigDecimal.valueOf(29.99))
                                .quantity(2)
                                .subtotal(BigDecimal.valueOf(59.98))
                                .build()
                ))
                .totalAmount(BigDecimal.valueOf(59.98))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentId(100L)
                .transactionId("TXN-12345")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
