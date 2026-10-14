package com.csci318.inventory.controller;

import com.csci318.inventory.event.StockChangedEvent;
import com.csci318.inventory.stream.InventoryStream;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

// Reads the live stock table built by InventoryStream (Kafka Streams interactive queries)
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InteractiveQueryService queryService;
    private final int threshold;

    public InventoryController(InteractiveQueryService queryService,
                               @Value("${inventory.low-stock-threshold}") int threshold) {
        this.queryService = queryService;
        this.threshold = threshold;
    }

    // Current stock of every product
    @GetMapping
    public List<StockChangedEvent> all() {
        List<StockChangedEvent> result = new ArrayList<>();
        try (KeyValueIterator<String, StockChangedEvent> it = store().all()) {
            it.forEachRemaining(kv -> result.add(kv.value));
        }
        return result;
    }

    // Products currently below the low-stock threshold
    @GetMapping("/low-stock")
    public List<StockChangedEvent> lowStock() {
        return all().stream().filter(e -> e.stock() < threshold).toList();
    }

    @GetMapping("/{productId}")
    public StockChangedEvent one(@PathVariable Long productId) {
        StockChangedEvent e = store().get(String.valueOf(productId));
        if (e == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no stock data for product");
        return e;
    }

    private ReadOnlyKeyValueStore<String, StockChangedEvent> store() {
        return queryService.getQueryableStore(InventoryStream.STOCK_STORE, QueryableStoreTypes.keyValueStore());
    }
}
