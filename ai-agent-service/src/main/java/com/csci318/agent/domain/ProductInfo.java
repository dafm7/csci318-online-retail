package com.csci318.agent.domain;

// The agent's view of a product (read from product-service). availability: IN_STOCK, LOW_STOCK, OUT_OF_STOCK
public record ProductInfo(Long id, String name, String category, double price, int stock, String availability) {}
