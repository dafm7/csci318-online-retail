package com.csci318.payment.domain.event;

import java.util.List;

public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount,
                               List<OrderLineEvent> items) {
    public OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount) {
        this(orderId, customerId, productId, quantity, amount, List.of());
    }
}
