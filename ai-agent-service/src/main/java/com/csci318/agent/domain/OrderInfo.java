package com.csci318.agent.domain;

// The agent's view of an order (read from order-service)
public record OrderInfo(Long id, Long customerId, Long productId, int quantity, double amount, String status) {}
