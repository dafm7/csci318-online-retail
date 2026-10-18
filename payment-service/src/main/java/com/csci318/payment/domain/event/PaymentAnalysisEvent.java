package com.csci318.payment.domain.event;

import java.time.Instant;
import java.math.BigDecimal;

public record PaymentAnalysisEvent(String eventId, Long orderId, String status, BigDecimal amount, Instant occurredAt) {}
