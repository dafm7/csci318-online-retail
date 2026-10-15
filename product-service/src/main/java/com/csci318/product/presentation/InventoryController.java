package com.csci318.product.presentation;

import com.csci318.product.domain.event.StockChangedEvent;
import com.csci318.product.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) { this.service = service; }

    // Current stock of every product
    @GetMapping
    public List<StockChangedEvent> all() { return service.all(); }

    // Products currently below the low-stock threshold
    @GetMapping("/low-stock")
    public List<StockChangedEvent> lowStock() { return service.lowStock(); }

    @GetMapping("/{productId}")
    public StockChangedEvent one(@PathVariable Long productId) { return service.get(productId); }
}
