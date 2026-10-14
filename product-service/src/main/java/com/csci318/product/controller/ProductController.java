package com.csci318.product.controller;

import com.csci318.product.model.Product;
import com.csci318.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    // Product list, optionally filtered: ?search=keyboard  ?category=Sport  (or both)
    @GetMapping
    public List<Product> list(@RequestParam(required = false) String search,
                              @RequestParam(required = false) String category) {
        return service.find(search, category);
    }

    @GetMapping("/{id}")
    public Product one(@PathVariable Long id) { return service.get(id); }

    // POST {"name","category","price","stock"}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product add(@RequestBody Product product) { return service.add(product); }
}
