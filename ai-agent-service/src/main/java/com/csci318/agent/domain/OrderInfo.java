package com.csci318.agent.domain;

import java.util.List;

// The agent's view of an order (read from order-service)
public record OrderInfo(Long id, Long customerId, Long productId, int quantity, double amount, String status,
                        List<OrderItemInfo> items) {
    public record OrderItemInfo(Long productId, int quantity, double unitPrice) {}
}
