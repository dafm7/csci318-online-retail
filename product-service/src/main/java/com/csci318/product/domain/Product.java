package com.csci318.product.domain;

import jakarta.persistence.*;

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

    protected Product() {}   // for JPA only

    private Product(String name, Category category, double price, int stock) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    // Factory: domain rules for a new product
    public static Product create(String name, Category category, double price, int stock) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (category == null) throw new IllegalArgumentException("category is required");
        if (price <= 0) throw new IllegalArgumentException("price must be greater than 0");
        if (stock < 0) throw new IllegalArgumentException("stock cannot be negative");
        return new Product(name, category, price, stock);
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
}
