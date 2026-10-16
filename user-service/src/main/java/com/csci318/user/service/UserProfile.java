package com.csci318.user.service;

import com.csci318.user.domain.Role;

import java.util.List;

// What "view profile" returns: the user's details plus a summary of their orders
public record UserProfile(Long id, String name, String email, Role role, OrderHistory orders) {

    // orders is null when order-service is unavailable
    public record OrderHistory(int totalOrders, int paidOrders, double totalSpent, List<RecentOrder> recentOrders) {}

    public record RecentOrder(Long orderId, Long productId, int quantity, double amount, String status) {}
}
