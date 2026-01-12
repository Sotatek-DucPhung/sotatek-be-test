package com.sotatek.order.service.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Product Stock from external Product Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStockDto {

    private Long productId;
    private Integer quantity;           // Total quantity in warehouse
    private Integer reservedQuantity;   // Quantity reserved for pending orders
    private Integer availableQuantity;  // Available for new orders
}
