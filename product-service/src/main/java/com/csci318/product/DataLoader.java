package com.csci318.product;

import com.csci318.product.model.Product;
import com.csci318.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Sample data so the app isn't empty on startup (H2 is in-memory)
@Component
public class DataLoader implements CommandLineRunner {
    private final ProductRepository repo;

    public DataLoader(ProductRepository repo) { this.repo = repo; }

    @Override
    public void run(String... args) {
        if (repo.count() == 0) {
            repo.save(new Product("Wireless Mouse", "Electronics", 29.99, 50));
            repo.save(new Product("Mechanical Keyboard", "Electronics", 119.00, 20));
            repo.save(new Product("Running Shoes", "Sport", 89.50, 35));
            repo.save(new Product("Coffee Mug", "Home", 12.00, 100));
        }
    }
}
