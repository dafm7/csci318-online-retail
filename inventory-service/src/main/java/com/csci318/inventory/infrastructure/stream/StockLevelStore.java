package com.csci318.inventory.infrastructure.stream;

import com.csci318.inventory.domain.event.StockChangedEvent;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Reads the live stock table built by InventoryStream (Kafka Streams interactive queries)
@Component
public class StockLevelStore {

    private final InteractiveQueryService queryService;

    public StockLevelStore(InteractiveQueryService queryService) { this.queryService = queryService; }

    public List<StockChangedEvent> findAll() {
        List<StockChangedEvent> result = new ArrayList<>();
        try (KeyValueIterator<String, StockChangedEvent> it = store().all()) {
            it.forEachRemaining(kv -> result.add(kv.value));
        }
        return result;
    }

    public Optional<StockChangedEvent> findByProductId(Long productId) {
        return Optional.ofNullable(store().get(String.valueOf(productId)));
    }

    private ReadOnlyKeyValueStore<String, StockChangedEvent> store() {
        return queryService.getQueryableStore(InventoryStream.STOCK_STORE, QueryableStoreTypes.keyValueStore());
    }
}
