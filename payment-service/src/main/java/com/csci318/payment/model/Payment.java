package com.csci318.payment.model;

import jakarta.persistence.*;

@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private double amount;
    private boolean success;

    public Payment() {}

    public Payment(Long orderId, double amount, boolean success) {
        this.orderId = orderId;
        this.amount = amount;
        this.success = success;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public double getAmount() { return amount; }
    public boolean isSuccess() { return success; }
}
