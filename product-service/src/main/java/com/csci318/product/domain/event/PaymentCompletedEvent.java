package com.csci318.product.domain.event;

// Copy of the event published by payment-service (each service keeps its own copy)
public record PaymentCompletedEvent(Long orderId, boolean success) {}
