package com.csci318.product.presentation;

import com.csci318.product.service.ProductCatalogue;
import com.csci318.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService service;

    public AdminProductController(ProductService service) { this.service = service; }

    // Admin only. Header X-User-Id: <admin's user id>. All products grouped by category
    @GetMapping
    public ProductCatalogue catalogue(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return service.catalogue(userId);
    }
}
