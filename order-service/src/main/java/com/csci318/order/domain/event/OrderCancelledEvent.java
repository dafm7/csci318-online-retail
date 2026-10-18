package com.csci318.order.domain.event;

import java.util.List;

public record OrderCancelledEvent(Long orderId, Long productId, int quantity, List<OrderLineEvent> items) {
    public OrderCancelledEvent(Long orderId, Long productId, int quantity) { this(orderId, productId, quantity, List.of()); }
    public List<OrderLineEvent> lines() {
        return items == null || items.isEmpty() ? List.of(new OrderLineEvent(productId, quantity, 0.0)) : items;
    }
}
