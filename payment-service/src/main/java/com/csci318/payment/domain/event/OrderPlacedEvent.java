package com.csci318.payment.domain.event;

import java.util.List;
import java.math.BigDecimal;

public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, BigDecimal amount,
                               List<OrderLineEvent> items) {
    public OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, BigDecimal amount) {
        this(orderId, customerId, productId, quantity, amount, List.of());
    }
}
