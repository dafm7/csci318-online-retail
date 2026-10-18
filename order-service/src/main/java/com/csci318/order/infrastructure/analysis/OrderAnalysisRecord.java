package com.csci318.order.infrastructure.analysis;

import com.csci318.order.domain.event.OrderAnalysisEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "order_analysis_event")
public class OrderAnalysisRecord {
    @Id private String eventId;
    private Long orderId;
    private String eventType;
    private String previousStatus;
    private String status;
    private Instant occurredAt;

    protected OrderAnalysisRecord() {}
    public OrderAnalysisRecord(OrderAnalysisEvent event) {
        eventId = event.eventId();
        orderId = event.orderId();
        eventType = event.eventType();
        previousStatus = event.previousStatus();
        status = event.status();
        occurredAt = event.occurredAt();
    }
    public String getEventId() { return eventId; }
    public Long getOrderId() { return orderId; }
    public String getEventType() { return eventType; }
    public String getPreviousStatus() { return previousStatus; }
    public String getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
}
