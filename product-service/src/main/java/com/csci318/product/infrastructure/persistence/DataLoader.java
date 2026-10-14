package com.csci318.product.infrastructure.persistence;

import com.csci318.product.infrastructure.messaging.StockEventPublisher;
import com.csci318.product.domain.Product;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Sample data so the app isn't empty on startup (H2 is in-memory)
@Component
public class DataLoader implements CommandLineRunner {
    private final ProductRepository repo;
    private final StockEventPublisher publisher;

    public DataLoader(ProductRepository repo, StockEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    @Override
    public void run(String... args) {
        if (repo.count() == 0) {
            repo.save(new Product("Wireless Mouse", "Electronics", 29.99, 50));
            repo.save(new Product("Mechanical Keyboard", "Electronics", 119.00, 20));
            repo.save(new Product("Running Shoes", "Sport", 89.50, 35));
            repo.save(new Product("Coffee Mug", "Home", 12.00, 100));
        }
        // Tell inventory-service the starting stock of every product
        repo.findAll().forEach(publisher::stockChanged);
    }
}
