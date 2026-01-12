package com.sotatek.order.service;

import com.sotatek.order.controller.request.CreateOrderRequest;
import com.sotatek.order.controller.request.OrderItemRequest;
import com.sotatek.order.controller.request.UpdateOrderRequest;
import com.sotatek.order.controller.response.OrderResponse;
import com.sotatek.order.controller.response.PageResponse;
import com.sotatek.order.domain.Order;
import com.sotatek.order.domain.OrderItem;
import com.sotatek.order.domain.OrderStatus;
import com.sotatek.order.domain.PaymentMethod;
import com.sotatek.order.exception.*;
import com.sotatek.order.repository.OrderRepository;
import com.sotatek.order.service.external.MemberServiceClient;
import com.sotatek.order.service.external.PaymentServiceClient;
import com.sotatek.order.service.external.ProductServiceClient;
import com.sotatek.order.service.external.dto.*;
import com.sotatek.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MemberServiceClient memberServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(1L);
            }
            return order;
        });
    }

    @Test
    void createOrderRejectsInactiveMember() {
        CreateOrderRequest request = buildCreateOrderRequest(1L, 2001L, 1);
        MemberDto member = MemberDto.builder()
                .id(1L)
                .name("Inactive Member")
                .status("INACTIVE")
                .build();

        when(memberServiceClient.getMember(1L)).thenReturn(member);

        assertThrows(MemberValidationException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrderRejectsUnavailableProduct() {
        CreateOrderRequest request = buildCreateOrderRequest(1L, 2001L, 1);
        when(memberServiceClient.getMember(1L)).thenReturn(activeMember(1L));
        when(productServiceClient.getProduct(2001L)).thenReturn(ProductDto.builder()
                .id(2001L)
                .name("Discontinued")
                .price(BigDecimal.valueOf(10.00))
                .status("DISCONTINUED")
                .build());

        assertThrows(ProductValidationException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrderRejectsInsufficientStock() {
        CreateOrderRequest request = buildCreateOrderRequest(1L, 2001L, 5);
        when(memberServiceClient.getMember(1L)).thenReturn(activeMember(1L));
        when(productServiceClient.getProduct(2001L)).thenReturn(availableProduct(2001L));
        when(productServiceClient.getProductStock(2001L)).thenReturn(ProductStockDto.builder()
                .productId(2001L)
                .availableQuantity(2)
                .reservedQuantity(0)
                .quantity(2)
                .build());

        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrderPropagatesPaymentFailure() {
        CreateOrderRequest request = buildCreateOrderRequest(1L, 2001L, 1);
        when(memberServiceClient.getMember(1L)).thenReturn(activeMember(1L));
        when(productServiceClient.getProduct(2001L)).thenReturn(availableProduct(2001L));
        when(productServiceClient.getProductStock(2001L)).thenReturn(ProductStockDto.builder()
                .productId(2001L)
                .availableQuantity(10)
                .reservedQuantity(0)
                .quantity(10)
                .build());
        when(paymentServiceClient.createPayment(any(PaymentRequestDto.class)))
                .thenThrow(new PaymentFailedException("Payment failed"));

        assertThrows(PaymentFailedException.class, () -> orderService.createOrder(request));
    }

    @Test
    void updateOrderRejectsInvalidStatusTransition() {
        Order order = Order.builder()
                .id(1L)
                .memberId(1L)
                .memberName("Member 1")
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findByIdWithItems(eq(1L))).thenReturn(Optional.of(order));

        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        assertThrows(InvalidOrderStatusException.class, () -> orderService.updateOrder(1L, request));
    }

    // ========== SUCCESS SCENARIOS ==========

    @Test
    void createOrderSuccessfully() {
        // Arrange
        CreateOrderRequest request = buildCreateOrderRequest(1L, 2001L, 2);

        when(memberServiceClient.getMember(1L)).thenReturn(activeMember(1L));
        when(productServiceClient.getProduct(2001L)).thenReturn(availableProduct(2001L));
        when(productServiceClient.getProductStock(2001L)).thenReturn(sufficientStock(2001L, 10));
        when(paymentServiceClient.createPayment(any(PaymentRequestDto.class)))
                .thenReturn(successfulPayment(1L));

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getMemberId()).isEqualTo(1L);
        assertThat(response.getMemberName()).isEqualTo("Member 1");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(2001L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getPaymentId()).isEqualTo(100L);
        assertThat(response.getTransactionId()).isEqualTo("TXN-12345");

        verify(orderRepository, times(2)).save(any(Order.class)); // Once for PENDING, once for CONFIRMED
        verify(paymentServiceClient).createPayment(any(PaymentRequestDto.class));
    }

    @Test
    void getOrderByIdSuccessfully() {
        // Arrange
        Order order = buildOrderWithItems(1L, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        // Act
        OrderResponse response = orderService.getOrderById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getMemberId()).isEqualTo(1L);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void getOrderByIdThrowsNotFound() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    void listOrdersWithPagination() {
        // Arrange
        List<Order> orders = List.of(
                buildOrderWithItems(1L, OrderStatus.CONFIRMED),
                buildOrderWithItems(2L, OrderStatus.PENDING)
        );
        Page<Order> page = new PageImpl<>(orders, PageRequest.of(0, 10), 2);

        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        PageResponse<OrderResponse> response = orderService.listOrders(null, null, PageRequest.of(0, 10));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getPage().getTotalElements()).isEqualTo(2);
        assertThat(response.getPage().getNumber()).isEqualTo(0);
        assertThat(response.getPage().getSize()).isEqualTo(10);
    }

    @Test
    void listOrdersFilteredByMemberId() {
        // Arrange
        List<Order> orders = List.of(buildOrderWithItems(1L, OrderStatus.CONFIRMED));
        Page<Order> page = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

        when(orderRepository.findByMemberId(eq(1L), any(Pageable.class))).thenReturn(page);

        // Act
        PageResponse<OrderResponse> response = orderService.listOrders(1L, null, PageRequest.of(0, 10));

        // Assert
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getMemberId()).isEqualTo(1L);
    }

    @Test
    void listOrdersFilteredByStatus() {
        // Arrange
        List<Order> orders = List.of(buildOrderWithItems(1L, OrderStatus.CONFIRMED));
        Page<Order> page = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

        when(orderRepository.findByStatus(eq(OrderStatus.CONFIRMED), any(Pageable.class))).thenReturn(page);

        // Act
        PageResponse<OrderResponse> response = orderService.listOrders(null, OrderStatus.CONFIRMED, PageRequest.of(0, 10));

        // Assert
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderItemsOnPendingOrder() {
        // Arrange
        Order order = buildOrderWithItems(1L, OrderStatus.PENDING);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
        when(productServiceClient.getProduct(2002L)).thenReturn(availableProduct(2002L));
        when(productServiceClient.getProductStock(2002L)).thenReturn(sufficientStock(2002L, 10));

        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .items(List.of(OrderItemRequest.builder()
                        .productId(2002L)
                        .quantity(5)
                        .build()))
                .build();

        // Act
        OrderResponse response = orderService.updateOrder(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(2002L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderRejectsItemUpdateOnConfirmedOrder() {
        // Arrange
        Order order = buildOrderWithItems(1L, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .items(List.of(OrderItemRequest.builder()
                        .productId(2001L)
                        .quantity(3)
                        .build()))
                .build();

        // Act & Assert
        assertThrows(InvalidOrderStatusException.class, () -> orderService.updateOrder(1L, request));
    }

    @Test
    void updateOrderCancelsConfirmedOrder() {
        // Arrange
        Order order = buildOrderWithItems(1L, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

        UpdateOrderRequest request = UpdateOrderRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build();

        // Act
        OrderResponse response = orderService.updateOrder(1L, request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(any(Order.class));
    }

    private CreateOrderRequest buildCreateOrderRequest(Long memberId, Long productId, int quantity) {
        return CreateOrderRequest.builder()
                .memberId(memberId)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .build()))
                .build();
    }

    private MemberDto activeMember(Long memberId) {
        return MemberDto.builder()
                .id(memberId)
                .name("Member " + memberId)
                .status("ACTIVE")
                .build();
    }

    private ProductDto availableProduct(Long productId) {
        return ProductDto.builder()
                .id(productId)
                .name("Product " + productId)
                .price(BigDecimal.valueOf(10.00))
                .status("AVAILABLE")
                .build();
    }

    private ProductStockDto sufficientStock(Long productId, int quantity) {
        return ProductStockDto.builder()
                .productId(productId)
                .availableQuantity(quantity)
                .reservedQuantity(0)
                .quantity(quantity)
                .build();
    }

    private PaymentDto successfulPayment(Long orderId) {
        return PaymentDto.builder()
                .id(100L)
                .orderId(orderId)
                .amount(BigDecimal.valueOf(20.00))
                .status("COMPLETED")
                .transactionId("TXN-12345")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private Order buildOrderWithItems(Long orderId, OrderStatus status) {
        Order order = Order.builder()
                .id(orderId)
                .memberId(1L)
                .memberName("Member 1")
                .status(status)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalAmount(BigDecimal.valueOf(10.00))
                .paymentId(100L)
                .transactionId("TXN-12345")
                .build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(2001L)
                .productName("Product 2001")
                .unitPrice(BigDecimal.valueOf(10.00))
                .quantity(1)
                .build();
        item.calculateSubtotal();

        order.addItem(item);
        order.calculateTotalAmount();

        return order;
    }
}
