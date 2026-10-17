package com.csci318.product.service;

import com.csci318.product.domain.Availability;
import com.csci318.product.domain.Category;

import java.util.List;

// Admin view of all products, grouped by category
public record ProductCatalogue(int totalProducts, int totalStock, List<CategoryGroup> categories) {

    public record CategoryGroup(Category category, int productCount, int totalStock, List<ProductLine> products) {}

    public record ProductLine(Long id, String name, double price, int stock, int lowStockThreshold,
                              Availability availability) {}
}
