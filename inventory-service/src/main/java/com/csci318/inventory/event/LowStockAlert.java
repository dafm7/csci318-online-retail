package com.csci318.inventory.event;

// Published to "low-stock-alerts" when a product's stock drops below the threshold
public record LowStockAlert(Long productId, String name, int stock, int threshold) {}
