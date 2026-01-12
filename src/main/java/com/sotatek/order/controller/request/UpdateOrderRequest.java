package com.sotatek.order.controller.request;

import com.sotatek.order.domain.OrderStatus;
import com.sotatek.order.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing order
 * Can update items, payment method, or status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {

    @Size(max = 100, message = "Order cannot contain more than 100 items")
    @Valid
    private List<OrderItemRequest> items;

    private PaymentMethod paymentMethod;

    /**
     * Order status - can be used to cancel order (CONFIRMED → CANCELLED)
     */
    private OrderStatus status;
}
