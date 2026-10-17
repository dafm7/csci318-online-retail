package com.csci318.product.presentation;

import com.csci318.product.domain.Category;
import com.csci318.product.domain.Product;
import com.csci318.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    public record AddProductRequest(String name, String category, double price, int stock) {}
    public record RestockRequest(int quantity) {}

    // Product list, optionally filtered: ?search=keyboard  ?category=electronics  (or both)
    @GetMapping
    public List<Product> list(@RequestParam(required = false) String search,
                              @RequestParam(required = false) String category) {
        return service.find(search, category);
    }

    @GetMapping("/categories")
    public List<Category> categories() { return service.categories(); }

    @GetMapping("/{id}")
    public Product one(@PathVariable Long id) { return service.get(id); }

    // Admin only. Header X-User-Id: <admin's user id>   Body {"name","category","price","stock"}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product add(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                       @RequestBody AddProductRequest req) {
        return service.add(userId, req.name(), req.category(), req.price(), req.stock());
    }

    // Admin only. Header X-User-Id: <admin's user id>   Body {"quantity": 50}
    @PostMapping("/{id}/restock")
    public Product restock(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                           @PathVariable Long id, @RequestBody RestockRequest req) {
        return service.restock(userId, id, req.quantity());
    }
}
