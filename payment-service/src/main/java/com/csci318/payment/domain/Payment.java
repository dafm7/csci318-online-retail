package com.csci318.payment.domain;

import jakarta.persistence.*;

@Entity
public class Payment {
    // Mock payment rule: anything up to $1000 succeeds. Replace with real logic.
    public static final double LIMIT = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private double amount;
    private boolean success;

    public Payment() {}

    private Payment(Long orderId, double amount, boolean success) {
        this.orderId = orderId;
        this.amount = amount;
        this.success = success;
    }

    // Domain rule: decides whether a payment for this amount succeeds
    public static Payment attempt(Long orderId, double amount) {
        return new Payment(orderId, amount, amount > 0 && amount <= LIMIT);
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public double getAmount() { return amount; }
    public boolean isSuccess() { return success; }
}
