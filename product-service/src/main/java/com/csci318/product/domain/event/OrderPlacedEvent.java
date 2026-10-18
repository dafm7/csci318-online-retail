package com.csci318.product.domain.event;

import java.util.List;

// Local event contract copy; legacy fields remain for existing readers.
public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount,
                               List<OrderLineEvent> items) {
    public OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount) {
        this(orderId, customerId, productId, quantity, amount, List.of());
    }
    public List<OrderLineEvent> lines() {
        return items == null || items.isEmpty()
                ? List.of(new OrderLineEvent(productId, quantity, amount / Math.max(quantity, 1))) : items;
    }
}
