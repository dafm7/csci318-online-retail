package com.csci318.product.infrastructure.persistence;

import com.csci318.product.domain.Category;
import com.csci318.product.domain.Product;
import com.csci318.product.infrastructure.messaging.StockEventPublisher;
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
            repo.save(Product.create("Wireless Mouse", Category.ELECTRONICS, 29.99, 50));
            repo.save(Product.create("Mechanical Keyboard", Category.ELECTRONICS, 119.00, 20));
            repo.save(Product.create("Running Shoes", Category.SPORTS, 89.50, 35));
            repo.save(Product.create("Coffee Mug", Category.HOME, 12.00, 100));
            repo.save(Product.create("Hoodie", Category.CLOTHES, 49.95, 40));
        }
        // Publish the starting stock of every product (feeds the live stock table)
        repo.findAll().forEach(publisher::stockChanged);
    }
}
