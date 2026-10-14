package com.csci318.agent.domain;

// The agent's view of a product (read from product-service)
public record ProductInfo(Long id, String name, String category, double price, int stock) {}
