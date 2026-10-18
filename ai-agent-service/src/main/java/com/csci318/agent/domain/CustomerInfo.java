package com.csci318.agent.domain;

// The agent's view of a customer profile (read from user-service, GET /api/users/{id}).
// orders is null when user-service could not reach order-service.
public record CustomerInfo(Long id, String name, String email, String role, OrderSummary orders) {

    public record OrderSummary(int totalOrders, int paidOrders, double totalSpent) {}
}
