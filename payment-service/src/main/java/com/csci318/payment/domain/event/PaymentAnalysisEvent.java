package com.csci318.payment.domain.event;

import java.time.Instant;

public record PaymentAnalysisEvent(String eventId, Long orderId, String status, double amount, Instant occurredAt) {}
