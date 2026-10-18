package com.csci318.order.domain.event;

import java.util.List;

public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount,
                               List<OrderLineEvent> items) {
    public OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount) {
        this(orderId, customerId, productId, quantity, amount, List.of());
    }
    public List<OrderLineEvent> lines() {
        return items == null || items.isEmpty()
                ? List.of(new OrderLineEvent(productId, quantity,
                    Math.round(amount / Math.max(quantity, 1) * 100) / 100.0)) : items;
    }
}
