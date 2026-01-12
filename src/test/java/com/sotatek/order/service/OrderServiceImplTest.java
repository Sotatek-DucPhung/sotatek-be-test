package com.sotatek.order.service;

import com.sotatek.order.controller.request.CreateOrderRequest;
import com.sotatek.order.controller.request.OrderItemRequest;
import com.sotatek.order.controller.request.UpdateOrderRequest;
import com.sotatek.order.domain.Order;
import com.sotatek.order.domain.OrderStatus;
import com.sotatek.order.domain.PaymentMethod;
import com.sotatek.order.exception.InsufficientStockException;
import com.sotatek.order.exception.InvalidOrderStatusException;
import com.sotatek.order.exception.MemberValidationException;
import com.sotatek.order.exception.PaymentFailedException;
import com.sotatek.order.exception.ProductValidationException;
import com.sotatek.order.repository.OrderRepository;
import com.sotatek.order.service.external.MemberServiceClient;
import com.sotatek.order.service.external.PaymentServiceClient;
import com.sotatek.order.service.external.ProductServiceClient;
import com.sotatek.order.service.external.dto.MemberDto;
import com.sotatek.order.service.external.dto.PaymentRequestDto;
import com.sotatek.order.service.external.dto.ProductDto;
import com.sotatek.order.service.external.dto.ProductStockDto;
import com.sotatek.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
}
