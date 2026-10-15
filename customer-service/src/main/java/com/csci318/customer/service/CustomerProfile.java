package com.csci318.customer.service;

import java.time.LocalDateTime;
import java.util.List;

// What "view profile" returns: the customer's details plus a summary of their orders
public record CustomerProfile(Long id, String name, String email, String address,
                              LocalDateTime memberSince, OrderHistory orders) {

    // orders is null when order-service is unavailable
    public record OrderHistory(int totalOrders, int paidOrders, double totalSpent, List<RecentOrder> recentOrders) {}

    public record RecentOrder(Long orderId, Long productId, int quantity, double amount, String status) {}
}
