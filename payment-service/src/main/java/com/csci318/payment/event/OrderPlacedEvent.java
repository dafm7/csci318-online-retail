package com.csci318.payment.event;

public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount) {}
