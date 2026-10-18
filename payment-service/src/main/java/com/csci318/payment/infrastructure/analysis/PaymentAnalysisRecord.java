package com.csci318.payment.infrastructure.analysis;

import com.csci318.payment.domain.event.PaymentAnalysisEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_analysis_event")
public class PaymentAnalysisRecord {
    @Id private String eventId;
    private Long orderId;
    private String status;
    private double amount;
    private Instant occurredAt;

    protected PaymentAnalysisRecord() {}
    public PaymentAnalysisRecord(PaymentAnalysisEvent event) {
        eventId = event.eventId();
        orderId = event.orderId();
        status = event.status();
        amount = event.amount();
        occurredAt = event.occurredAt();
    }
    public String getEventId() { return eventId; }
    public Long getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public double getAmount() { return amount; }
    public Instant getOccurredAt() { return occurredAt; }
}
