package com.csci318.product.service;

import com.csci318.product.domain.Product;
import com.csci318.product.domain.event.OrderPlacedEvent;
import com.csci318.product.infrastructure.messaging.StockEventPublisher;
import com.csci318.product.infrastructure.persistence.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Use cases for products; the controller and event handler only pass requests in
@Service
public class ProductService {

    private final ProductRepository repo;
    private final StockEventPublisher publisher;

    public ProductService(ProductRepository repo, StockEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    public Product add(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (product.getPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be greater than 0");
        }
        if (product.getStock() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stock cannot be negative");
        }
        Product saved = repo.save(product);
        publisher.stockChanged(saved);
        return saved;
    }

    // No filters -> all products; search matches part of the name; category is an exact match
    public List<Product> find(String search, String category) {
        List<Product> products = (search != null) ? repo.findByNameContainingIgnoreCase(search)
                : (category != null) ? repo.findByCategoryIgnoreCase(category)
                : repo.findAll();
        if (search != null && category != null) {
            products = products.stream().filter(p -> category.equalsIgnoreCase(p.getCategory())).toList();
        }
        return products;
    }

    public Product get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found"));
    }

    // Called for every OrderPlacedEvent: reduce stock and announce the new level
    public void reduceStockForOrder(OrderPlacedEvent event) {
        repo.findById(event.productId()).ifPresent(p -> {
            if (!p.hasStock(event.quantity())) {
                System.out.println("[product-service] NOT ENOUGH STOCK for product " + p.getId()
                        + " (order " + event.orderId() + "): " + p.getStock() + " left, " + event.quantity() + " requested");
                return;
            }
            p.reduceStock(event.quantity());
            repo.save(p);
            publisher.stockChanged(p);
            System.out.println("[product-service] stock reduced for product " + p.getId() + " -> " + p.getStock());
        });
    }
}
