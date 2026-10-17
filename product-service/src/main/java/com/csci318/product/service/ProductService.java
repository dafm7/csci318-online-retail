package com.csci318.product.service;

import com.csci318.product.domain.Category;
import com.csci318.product.domain.LowStockPolicy;
import com.csci318.product.domain.Product;
import com.csci318.product.domain.event.OrderPlacedEvent;
import com.csci318.product.infrastructure.messaging.StockEventPublisher;
import com.csci318.product.infrastructure.persistence.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// Use cases for products; the controllers and event handler only pass requests in
@Service
public class ProductService {

    private final ProductRepository repo;
    private final StockEventPublisher publisher;
    private final AdminAccess adminAccess;
    private final LowStockPolicy lowStockPolicy;

    public ProductService(ProductRepository repo, StockEventPublisher publisher,
                          AdminAccess adminAccess, LowStockPolicy lowStockPolicy) {
        this.repo = repo;
        this.publisher = publisher;
        this.adminAccess = adminAccess;
        this.lowStockPolicy = lowStockPolicy;
    }

    // Admin only
    public Product add(Long userId, String name, String category, double price, int stock) {
        adminAccess.requireAdmin(userId);
        Product saved = repo.save(domainRule(() -> Product.create(name, Category.from(category), price, stock)));
        publisher.stockChanged(saved);
        return saved;
    }

    // Admin only: add stock to an existing product and announce the new level
    public Product restock(Long userId, Long productId, int quantity) {
        adminAccess.requireAdmin(userId);
        Product p = get(productId);
        domainRule(() -> { p.restock(quantity); return p; });
        Product saved = repo.save(p);
        publisher.stockChanged(saved);
        System.out.println("[product-service] restocked product " + saved.getId() + " (+" + quantity + ") -> " + saved.getStock());
        return saved;
    }

    // No filters -> all products; search matches part of the name; category is one of the 4 categories
    public List<Product> find(String search, String category) {
        Category cat = (category == null) ? null : domainRule(() -> Category.from(category));
        List<Product> products = (search != null) ? repo.findByNameContainingIgnoreCase(search)
                : (cat != null) ? repo.findByCategory(cat)
                : repo.findAll();
        if (search != null && cat != null) {
            products = products.stream().filter(p -> p.getCategory() == cat).toList();
        }
        return products;
    }

    public Product get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found"));
    }

    public List<Category> categories() { return Arrays.asList(Category.values()); }

    // Admin only: every product grouped by category (all 4 categories, even empty ones)
    public ProductCatalogue catalogue(Long userId) {
        adminAccess.requireAdmin(userId);
        List<Product> all = repo.findAll();
        List<ProductCatalogue.CategoryGroup> groups = Arrays.stream(Category.values()).map(cat -> {
            List<ProductCatalogue.ProductLine> lines = all.stream()
                    .filter(p -> p.getCategory() == cat)
                    .sorted(Comparator.comparing(Product::getName))
                    .map(p -> new ProductCatalogue.ProductLine(p.getId(), p.getName(), p.getPrice(), p.getStock(),
                            lowStockPolicy.isLow(p.getStock())))
                    .toList();
            return new ProductCatalogue.CategoryGroup(cat, lines.size(),
                    lines.stream().mapToInt(ProductCatalogue.ProductLine::stock).sum(), lines);
        }).toList();
        return new ProductCatalogue(all.size(), all.stream().mapToInt(Product::getStock).sum(), groups);
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

    // Turns a broken domain rule (IllegalArgumentException) into a 400 Bad Request
    private static <T> T domainRule(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
