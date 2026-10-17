package com.csci318.product.domain.event;

// Published to "stock-changed" whenever a product's stock changes (used by the stock streams)
// lowStockThreshold is null in events published before per-product thresholds existed
public record StockChangedEvent(Long productId, String name, int stock, Integer lowStockThreshold) {}
