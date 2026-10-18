package com.csci318.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_refund_payment_id", columnNames = "payment_id"))
public class Refund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long paymentId;
    private Long orderId;
    private double amount;
    private String status;
    private Instant createdAt;
    protected Refund() {}

    public static Refund completed(Payment payment) {
        Refund refund = new Refund();
        refund.paymentId = payment.getId();
        refund.orderId = payment.getOrderId();
        refund.amount = payment.getAmount();
        refund.status = "COMPLETED";
        refund.createdAt = Instant.now();
        return refund;
    }
    public Long getId() { return id; }
    public Long getPaymentId() { return paymentId; }
    public Long getOrderId() { return orderId; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
