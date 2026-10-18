package com.csci318.agent.domain;

// The agent's view of a customer profile (read from customer-service).
// orders is null when customer-service could not reach order-service.
public record CustomerInfo(Long id, String name, String email, String address, OrderSummary orders) {

    public record OrderSummary(int totalOrders, int paidOrders, double totalSpent) {}
}
