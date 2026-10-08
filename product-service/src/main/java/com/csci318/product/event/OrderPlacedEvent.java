package com.csci318.product.event;

// Copy of the event published by order-service (each service keeps its own copy)
public record OrderPlacedEvent(Long orderId, Long customerId, Long productId, int quantity, double amount) {}
