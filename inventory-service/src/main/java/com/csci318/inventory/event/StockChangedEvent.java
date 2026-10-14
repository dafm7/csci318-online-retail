package com.csci318.inventory.event;

// Copy of the event published by product-service (each service keeps its own copy)
public record StockChangedEvent(Long productId, String name, int stock) {}
