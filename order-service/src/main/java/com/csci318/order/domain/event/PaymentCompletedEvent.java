package com.csci318.order.domain.event;

public record PaymentCompletedEvent(Long orderId, boolean success) {}
