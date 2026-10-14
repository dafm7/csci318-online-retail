package com.csci318.product.domain;

import jakarta.persistence.*;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private double price;
    private int stock;

    public Product() {}

    public Product(String name, String category, double price, int stock) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    // Domain rule: stock can never go below zero
    public boolean hasStock(int quantity) { return quantity > 0 && quantity <= stock; }

    public void reduceStock(int quantity) {
        if (!hasStock(quantity)) {
            throw new IllegalStateException("not enough stock for product " + id + ": " + stock + " left, " + quantity + " requested");
        }
        stock -= quantity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
