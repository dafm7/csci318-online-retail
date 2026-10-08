package com.csci318.order.event;

public record PaymentCompletedEvent(Long orderId, boolean success) {}
