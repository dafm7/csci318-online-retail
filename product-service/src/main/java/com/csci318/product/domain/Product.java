package com.csci318.product.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// Aggregate root of the product catalogue context.
// Created only through Product.create(...), so every product starts in a valid state.
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)
    private Category category;
    private double price;
    private int stock;
    private int lowStockThreshold;   // stock below this counts as LOW_STOCK

    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    protected Product() {}   

    private Product(String name, Category category, double price, int stock, int lowStockThreshold) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.lowStockThreshold = lowStockThreshold;
    }

    // Factory: domain rules for a new product
    public static Product create(String name, Category category, double price, int stock, int lowStockThreshold) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (category == null) throw new IllegalArgumentException("category is required");
        if (price <= 0) throw new IllegalArgumentException("price must be greater than 0");
        if (stock < 0) throw new IllegalArgumentException("stock cannot be negative");
        if (lowStockThreshold < 0) throw new IllegalArgumentException("lowStockThreshold cannot be negative");
        return new Product(name, category, price, stock, lowStockThreshold);
    }

    public static Product create(String name, Category category, double price, int stock) {
        return create(name, category, price, stock, DEFAULT_LOW_STOCK_THRESHOLD);
    }

    // Domain rule: low on stock when below this product's own threshold
    @JsonIgnore   // shown in JSON as "availability" instead
    public boolean isLowStock() { return stock < lowStockThreshold; }

    // Derived from stock, so it can never disagree with it
    public Availability getAvailability() {
        if (stock == 0) return Availability.OUT_OF_STOCK;
        return isLowStock() ? Availability.LOW_STOCK : Availability.IN_STOCK;
    }

    // Domain rule: stock can never go below zero
    public boolean hasStock(int quantity) { return quantity > 0 && quantity <= stock; }

    public void reduceStock(int quantity) {
        if (!hasStock(quantity)) {
            throw new IllegalStateException("not enough stock for product " + id + ": " + stock + " left, " + quantity + " requested");
        }
        stock -= quantity;
    }

    // Domain rule: restocking must add at least one unit
    public void restock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("restock quantity must be greater than 0");
        stock += quantity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public int getLowStockThreshold() { return lowStockThreshold; }
}
