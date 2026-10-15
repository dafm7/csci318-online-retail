package com.csci318.product.presentation;

import com.csci318.product.service.SalesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesService service;

    public SalesController(SalesService service) { this.service = service; }

    // Total units sold, revenue and orders per product (paid orders only), best sellers first
    @GetMapping
    public List<SalesService.ProductSalesView> totals() { return service.totals(); }

    // Units sold per product in each 1-minute window: ?minutes=5 looks back 5 minutes
    @GetMapping("/per-minute")
    public List<SalesService.MinuteSalesView> perMinute(@RequestParam(defaultValue = "5") int minutes) {
        return service.perMinute(minutes);
    }
}
