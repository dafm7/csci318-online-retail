package com.csci318.order.domain;

import jakarta.persistence.*;

// Named CustomerOrder because ORDER is a reserved word in SQL
@Entity
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private Long productId;
    private int quantity;
    private double amount;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;   // PENDING -> PAID / FAILED

    public CustomerOrder() {}

    public boolean awaitingPayment() { return status == OrderStatus.PENDING; }

    // Domain rule: only a PENDING order can receive a payment result
    public void recordPayment(boolean success) {
        if (!awaitingPayment()) {
            throw new IllegalStateException("order " + id + " is already " + status);
        }
        status = success ? OrderStatus.PAID : OrderStatus.FAILED;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public OrderStatus getStatus() { return status; }
}
