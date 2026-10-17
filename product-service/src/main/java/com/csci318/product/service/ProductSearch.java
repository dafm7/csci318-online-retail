package com.csci318.product.service;

// Customer product search: every field is optional
//   search      part of the product name (case-insensitive)
//   category    CLOTHES, ELECTRONICS, SPORTS or HOME
//   minPrice    / maxPrice   price range, inclusive
//   inStock     true = hide products with 0 stock
//   sort        price, price_desc, name, name_desc, stock, stock_desc
public record ProductSearch(String search, String category, Double minPrice, Double maxPrice,
                            boolean inStock, String sort) {}
