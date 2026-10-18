package com.csci318.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_payment_order_id", columnNames = "order_id"))
public class Payment {
    // Mock payment rule: anything up to $1000 succeeds. Replace with real logic.
    public static final BigDecimal LIMIT = new BigDecimal("1000.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO.setScale(2);
    private boolean success;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    @Version
    private Long version;

    public Payment() {}

    private Payment(Long orderId, BigDecimal amount, boolean success) {
        this.orderId = orderId;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.success = success;
    }

    // Domain rule: decides whether a payment for this amount succeeds
    public static Payment attempt(Long orderId, BigDecimal amount) {
        return new Payment(orderId, amount, false);
    }

    public static Payment pending(Long orderId, BigDecimal amount) { return new Payment(orderId, amount, false); }
    public void complete() { success = amount.signum() > 0 && amount.compareTo(LIMIT) <= 0; status = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED; updatedAt = Instant.now(); }
    public void refund() {
        if (status != PaymentStatus.SUCCESS) throw new IllegalStateException("only successful payments can be refunded");
        status = PaymentStatus.REFUNDED;
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public boolean isSuccess() { return success; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
