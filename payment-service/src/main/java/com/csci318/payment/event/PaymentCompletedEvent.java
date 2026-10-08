package com.csci318.payment.event;

public record PaymentCompletedEvent(Long orderId, boolean success) {}
