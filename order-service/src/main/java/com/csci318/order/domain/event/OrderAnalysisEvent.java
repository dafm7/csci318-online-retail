package com.csci318.order.domain.event;

import java.time.Instant;

public record OrderAnalysisEvent(String eventId, Long orderId, String eventType,
                                 String previousStatus, String status, Instant occurredAt) {}
