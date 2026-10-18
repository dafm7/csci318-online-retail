package com.csci318.product.service;

import com.csci318.product.domain.Category;
import com.csci318.product.domain.Product;
import com.csci318.product.domain.event.OrderPlacedEvent;
import com.csci318.product.domain.event.OrderCancelledEvent;
import com.csci318.product.infrastructure.messaging.StockEventPublisher;
import com.csci318.product.infrastructure.persistence.ProductRepository;
import org.springframework.data.domain.Sort;
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

    public ProductService(ProductRepository repo, StockEventPublisher publisher, AdminAccess adminAccess) {
        this.repo = repo;
        this.publisher = publisher;
        this.adminAccess = adminAccess;
    }

    // Admin only
    // lowStockThreshold is optional (null -> default of 10)
    public Product add(Long userId, String name, String category, double price, int stock, Integer lowStockThreshold) {
        adminAccess.requireAdmin(userId);
        int threshold = (lowStockThreshold == null) ? Product.DEFAULT_LOW_STOCK_THRESHOLD : lowStockThreshold;
        Product saved = repo.save(domainRule(() -> Product.create(name, Category.from(category), price, stock, threshold)));
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

    // Customer search: any combination of name, category, price range, in-stock and sort order
    public List<Product> find(ProductSearch q) {
        Category category = (q.category() == null) ? null : domainRule(() -> Category.from(q.category()));
        if (q.minPrice() != null && q.minPrice() < 0 || q.maxPrice() != null && q.maxPrice() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prices cannot be negative");
        }
        if (q.minPrice() != null && q.maxPrice() != null && q.minPrice() > q.maxPrice()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice cannot be greater than maxPrice");
        }
        String search = (q.search() == null || q.search().isBlank()) ? null : q.search().trim();
        return repo.search(search, category, q.minPrice(), q.maxPrice(), q.inStock(), sortOrder(q.sort()));
    }

    private static Sort sortOrder(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by("id");
        return switch (sort.trim().toLowerCase()) {
            case "price", "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "name", "name_asc" -> Sort.by("name").ascending();
            case "name_desc" -> Sort.by("name").descending();
            case "stock", "stock_asc" -> Sort.by("stock").ascending();
            case "stock_desc" -> Sort.by("stock").descending();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sort must be one of price, price_desc, name, name_desc, stock, stock_desc");
        };
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
                            p.getLowStockThreshold(), p.getAvailability()))
                    .toList();
            return new ProductCatalogue.CategoryGroup(cat, lines.size(),
                    lines.stream().mapToInt(ProductCatalogue.ProductLine::stock).sum(), lines);
        }).toList();
        return new ProductCatalogue(all.size(), all.stream().mapToInt(Product::getStock).sum(), groups);
    }

    // Called for every OrderPlacedEvent: reduce stock and announce the new level
    public void reduceStockForOrder(OrderPlacedEvent event) {
        var lines = event.lines();
        var products = new java.util.LinkedHashMap<Product, Integer>();
        for (var line : lines) {
            Product p = repo.findById(line.productId()).orElse(null);
            if (p == null || !p.hasStock(line.quantity())) {
                System.out.println("[product-service] unable to reserve product " + line.productId()
                        + " for order " + event.orderId() + "; order was accepted against an earlier stock snapshot");
                return;
            }
            products.merge(p, line.quantity(), Integer::sum);
        }
        for (var entry : products.entrySet()) {
            Product p = entry.getKey();
            if (!p.hasStock(entry.getValue())) {
                System.out.println("[product-service] insufficient stock for product " + p.getId() + " in order " + event.orderId());
                return;
            }
        }
        for (var entry : products.entrySet()) {
            Product p = entry.getKey();
            p.reduceStock(entry.getValue());
            repo.save(p);
            publisher.stockChanged(p);
        }
    }

    public void restoreStockForCancelledOrder(OrderCancelledEvent event) {
        for (var line : event.lines()) repo.findById(line.productId()).ifPresent(p -> {
            p.restock(line.quantity());
            repo.save(p);
            publisher.stockChanged(p);
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
