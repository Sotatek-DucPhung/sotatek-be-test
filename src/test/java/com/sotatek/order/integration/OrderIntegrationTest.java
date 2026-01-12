package com.sotatek.order.integration;

import com.sotatek.order.controller.request.CreateOrderRequest;
import com.sotatek.order.controller.request.OrderItemRequest;
import com.sotatek.order.controller.request.UpdateOrderRequest;
import com.sotatek.order.controller.response.OrderResponse;
import com.sotatek.order.domain.OrderStatus;
import com.sotatek.order.domain.PaymentMethod;
import com.sotatek.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests with full Spring context
 * Tests end-to-end flow with real database (H2) and mock external services
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        orderRepository.deleteAll();
    }

    @Test
    void createOrderEndToEnd() {
        // Arrange
        CreateOrderRequest request = CreateOrderRequest.builder()
                .memberId(1L) // Mock will return active member
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(2001L)
                                .quantity(2)
                                .build()
                ))
                .build();

        // Act
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "/api/orders",
                request,
                OrderResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getBody().getMemberId()).isEqualTo(1L);
        assertThat(response.getBody().getMemberName()).contains("Mock Member");
        assertThat(response.getBody().getItems()).hasSize(1);
        assertThat(response.getBody().getPaymentId()).isNotNull();
        assertThat(response.getBody().getTransactionId()).isNotNull();

        // Verify database persistence
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void createOrderFailsForInactiveMember() {
        // Arrange - memberId 8888 returns INACTIVE member in mock
        CreateOrderRequest request = CreateOrderRequest.builder()
                .memberId(8888L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(2001L)
                                .quantity(1)
                                .build()
                ))
                .build();

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/orders",
                request,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("MEMBER_VALIDATION_ERROR");

        // Verify no order was created in database
        assertThat(orderRepository.count()).isEqualTo(0);
    }

    @Test
    void createOrderFailsForNonExistentMember() {
        // Arrange - memberId 9999 throws MemberNotFoundException in mock
        CreateOrderRequest request = CreateOrderRequest.builder()
                .memberId(9999L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(2001L)
                                .quantity(1)
                                .build()
                ))
                .build();

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/orders",
                request,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("MEMBER_NOT_FOUND");
    }

    @Test
    void createOrderFailsForInsufficientStock() {
        // Arrange - productId 7777 has only 2 units in stock in mock
        CreateOrderRequest request = CreateOrderRequest.builder()
                .memberId(1L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(7777L)
                                .quantity(5) // Requesting more than available
                                .build()
                ))
                .build();

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/orders",
                request,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("INSUFFICIENT_STOCK");
    }

    @Test
    void getOrderById() {
        // Arrange - create an order first
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .memberId(1L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(2001L)
                                .quantity(1)
                                .build()
                ))
                .build();

        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "/api/orders",
                createRequest,
                OrderResponse.class
        );
        Long orderId = createResponse.getBody().getId();

        // Act
        ResponseEntity<OrderResponse> getResponse = restTemplate.getForEntity(
                "/api/orders/" + orderId,
                OrderResponse.class
        );

        // Assert
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getId()).isEqualTo(orderId);
        assertThat(getResponse.getBody().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void getOrderByIdReturns404ForNonExistent() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/orders/99999",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("ORDER_NOT_FOUND");
    }

    @Test
    void listOrdersWithPagination() {
        // Arrange - create multiple orders
        for (int i = 0; i < 3; i++) {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .memberId(1L)
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .items(List.of(
                            OrderItemRequest.builder()
                                    .productId(2001L)
                                    .quantity(1)
                                    .build()
                    ))
                    .build();

            restTemplate.postForEntity("/api/orders", request, OrderResponse.class);
        }

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/orders?page=0&size=10",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"content\"");
        assertThat(response.getBody()).contains("\"page\"");
    }

    @Test
    void updateOrderCancelsConfirmedOrder() {
        // Arrange - create a confirmed order first
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .memberId(1L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(2001L)
                                .quantity(1)
                                .build()
                ))
                .build();

        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "/api/orders",
                createRequest,
                OrderResponse.class
        );
        Long orderId = createResponse.getBody().getId();

        // Verify order is CONFIRMED
        assertThat(createResponse.getBody().getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Act - cancel the order
        UpdateOrderRequest updateRequest = UpdateOrderRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        ResponseEntity<OrderResponse> updateResponse = restTemplate.exchange(
                "/api/orders/" + orderId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                OrderResponse.class
        );

        // Assert
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Verify in database
        orderRepository.findById(orderId).ifPresent(order ->
                assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED)
        );
    }

    @Test
    void updateOrderRejectsInvalidStatusTransition() {
        // Arrange - create a PENDING order
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .memberId(1L)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(2001L)
                                .quantity(1)
                                .build()
                ))
                .build();

        // Mock will create CONFIRMED order automatically, but we can test with orderId=6666
        // which will fail payment and stay PENDING

        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "/api/orders",
                createRequest,
                OrderResponse.class
        );
        Long orderId = createResponse.getBody().getId();

        // This order is CONFIRMED, try to cancel it (valid)
        // Then try to change CANCELLED back to PENDING (invalid)
        UpdateOrderRequest cancelRequest = UpdateOrderRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        restTemplate.exchange(
                "/api/orders/" + orderId,
                HttpMethod.PUT,
                new HttpEntity<>(cancelRequest),
                OrderResponse.class
        );

        // Act - try to change CANCELLED back to PENDING (should fail)
        UpdateOrderRequest invalidRequest = UpdateOrderRequest.builder()
                .status(OrderStatus.PENDING)
                .build();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/orders/" + orderId,
                HttpMethod.PUT,
                new HttpEntity<>(invalidRequest),
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("INVALID_ORDER_STATUS");
    }
}
