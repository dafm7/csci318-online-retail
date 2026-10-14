package com.csci318.payment.domain.event;

public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount) {}
