package com.csci318.product.domain.event;

// Published to "stock-changed" whenever a product's stock changes (used by the stock streams)
public record StockChangedEvent(Long productId, String name, int stock) {}
