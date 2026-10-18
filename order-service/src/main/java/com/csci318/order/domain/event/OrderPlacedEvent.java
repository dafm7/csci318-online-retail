package com.csci318.order.domain.event;

import java.util.List;
import java.math.BigDecimal;

public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, BigDecimal amount,
                               List<OrderLineEvent> items) {
    public OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, BigDecimal amount) {
        this(orderId, customerId, productId, quantity, amount, List.of());
    }
    public List<OrderLineEvent> lines() {
        return items == null || items.isEmpty()
                ? List.of(new OrderLineEvent(productId, quantity,
                    amount.divide(BigDecimal.valueOf(Math.max(quantity, 1)), 2, java.math.RoundingMode.HALF_UP))) : items;
    }
}
