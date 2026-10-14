package com.csci318.product.event;

// Published to "stock-changed" whenever a product's stock changes (consumed by inventory-service)
public record StockChangedEvent(Long productId, String name, int stock) {}
