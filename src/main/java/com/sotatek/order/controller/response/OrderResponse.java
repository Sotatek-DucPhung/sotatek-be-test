package com.sotatek.order.controller.response;

import com.sotatek.order.domain.OrderStatus;
import com.sotatek.order.domain.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private Long memberId;
    private String memberName;
    private OrderStatus status;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private Long paymentId;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
