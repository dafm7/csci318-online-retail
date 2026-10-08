package com.csci318.order.event;

public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount) {}
