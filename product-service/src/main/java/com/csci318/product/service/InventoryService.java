package com.csci318.product.service;

import com.csci318.product.domain.LowStockPolicy;
import com.csci318.product.domain.event.StockChangedEvent;
import com.csci318.product.infrastructure.stream.StockLevelStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Use cases for viewing live inventory
@Service
public class InventoryService {

    private final StockLevelStore store;
    private final LowStockPolicy policy;

    public InventoryService(StockLevelStore store, LowStockPolicy policy) {
        this.store = store;
        this.policy = policy;
    }

    public List<StockChangedEvent> all() { return store.findAll(); }

    public List<StockChangedEvent> lowStock() { return store.findAll().stream().filter(policy::isLow).toList(); }

    public StockChangedEvent get(Long productId) {
        return store.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no stock data for product"));
    }
}
