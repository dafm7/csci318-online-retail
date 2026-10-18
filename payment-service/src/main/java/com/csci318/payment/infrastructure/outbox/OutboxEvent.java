package com.csci318.payment.infrastructure.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_outbox_event")
public class OutboxEvent {
    @Id private String id;
    private String bindingName;
    @Lob private String payload;
    private Instant createdAt;
    private Instant publishedAt;
    private int attempts;

    protected OutboxEvent() {}
    public OutboxEvent(String id, String bindingName, String payload, Instant createdAt) {
        this.id = id;
        this.bindingName = bindingName;
        this.payload = payload;
        this.createdAt = createdAt;
    }
    public void markPublished(Instant time) { publishedAt = time; }
    public void recordAttempt() { attempts++; }
    public String getId() { return id; }
    public String getBindingName() { return bindingName; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
}
