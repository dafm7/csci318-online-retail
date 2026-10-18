package com.csci318.order.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Named CustomerOrder because ORDER is a reserved word in SQL.
@Entity
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    // Kept as summary fields for the existing AI client and old single-item API.
    private Long productId;
    private int quantity;
    private double amount;   // order total in dollars, rounded to cents
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant refundedAt;
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public CustomerOrder() {}
    public boolean awaitingPayment() { return status == OrderStatus.PENDING; }

    public void setItems(List<OrderItem> lines) {
        items.clear();
        productId = null;
        quantity = 0;
        amount = 0;
        if (lines != null) lines.forEach(this::addItem);
    }
    public void addItem(OrderItem item) {
        if (item == null) throw new IllegalArgumentException("order item is required");
        if (item.getProductId() == null || item.getQuantity() <= 0 || item.getUnitPrice() < 0)
            throw new IllegalArgumentException("order item needs a product, positive quantity and non-negative price");
        if (quantity > Integer.MAX_VALUE - item.getQuantity()) throw new IllegalArgumentException("order quantity is too large");
        item.setOrder(this);
        items.add(item);
        if (items.size() == 1) productId = item.getProductId();
        quantity += item.getQuantity();
        amount = Math.round((amount + item.getLineTotal()) * 100) / 100.0;
    }
    public void cancel() {
        if (status != OrderStatus.PENDING && status != OrderStatus.PAID && status != OrderStatus.FAILED)
            throw new IllegalStateException("only pending, paid or failed orders can be cancelled");
        status = OrderStatus.CANCELLED;
        updatedAt = Instant.now();
    }
    public void updateStatus(OrderStatus next) {
        boolean allowed = (status == OrderStatus.PAID && next == OrderStatus.SHIPPED)
                || (status == OrderStatus.SHIPPED && next == OrderStatus.DELIVERED);
        if (!allowed) throw new IllegalStateException("status can only move PAID -> SHIPPED -> DELIVERED");
        status = next;
        updatedAt = Instant.now();
    }
    public void recordPayment(boolean success) {
        if (!awaitingPayment()) throw new IllegalStateException("order " + id + " is already " + status);
        status = success ? OrderStatus.PAID : OrderStatus.FAILED;
        updatedAt = Instant.now();
    }
    public void markRefunded() {
        if (status != OrderStatus.PAID && status != OrderStatus.CANCELLED)
            throw new IllegalStateException("only paid or cancelled orders can have a completed refund");
        // Preserve CANCELLED so cancellation-rate metrics still include the order.
        if (status == OrderStatus.PAID) status = OrderStatus.REFUNDED;
        refundedAt = Instant.now();
        updatedAt = Instant.now();
    }
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = Math.round(amount * 100) / 100.0; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getRefundedAt() { return refundedAt; }
    public OrderStatus getStatus() { return status; }
    public List<OrderItem> getItems() { return items; }
}
