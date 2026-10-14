package com.csci318.payment.domain.event;

public record PaymentCompletedEvent(Long orderId, boolean success) {}
