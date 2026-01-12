package com.sotatek.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sotatek.order.controller.request.CreateOrderRequest;
import com.sotatek.order.controller.request.OrderItemRequest;
import com.sotatek.order.controller.request.UpdateOrderRequest;
import com.sotatek.order.domain.OrderStatus;
import com.sotatek.order.domain.PaymentMethod;
import com.sotatek.order.exception.ExternalServiceException;
import com.sotatek.order.exception.GlobalExceptionHandler;
import com.sotatek.order.exception.InvalidOrderStatusException;
import com.sotatek.order.exception.MemberNotFoundException;
import com.sotatek.order.exception.OrderNotFoundException;
import com.sotatek.order.exception.PaymentFailedException;
import com.sotatek.order.exception.ProductValidationException;
import com.sotatek.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.mock.mockito.MockBean;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerExceptionHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrderReturnsNotFoundWhenMemberMissing() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new MemberNotFoundException(1L));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateOrderRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void createOrderReturnsBadRequestWhenProductInvalid() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new ProductValidationException("Product is not available"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateOrderRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PRODUCT_VALIDATION_ERROR"));
    }

    @Test
    void createOrderReturnsUnprocessableWhenPaymentFails() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new PaymentFailedException("Payment failed"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateOrderRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"));
    }

    @Test
    void createOrderReturnsServiceUnavailableWhenExternalFails() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new ExternalServiceException("Member service unavailable"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateOrderRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_UNAVAILABLE"));
    }

    @Test
    void updateOrderReturnsBadRequestWhenStatusInvalid() throws Exception {
        when(orderService.updateOrder(eq(1L), any(UpdateOrderRequest.class)))
                .thenThrow(new InvalidOrderStatusException("Invalid status"));

        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateOrderRequest.builder()
                                .status(OrderStatus.CANCELLED)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
    }

    @Test
    void getOrderReturnsNotFoundWhenMissing() throws Exception {
        when(orderService.getOrderById(1L))
                .thenThrow(new OrderNotFoundException(1L));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    private CreateOrderRequest validCreateOrderRequest() {
        return CreateOrderRequest.builder()
                .memberId(1L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder()
                        .productId(2001L)
                        .quantity(1)
                        .build()))
                .build();
    }
}
