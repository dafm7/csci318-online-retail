package com.csci318.product.infrastructure.stream;

import com.csci318.product.domain.ProductSales;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Reads the sales state stores built by SalesStream (Kafka Streams interactive queries)
@Component
public class SalesStore {

    public record WindowCount(Long productId, Instant windowStart, Instant windowEnd, long units) {}

    private final InteractiveQueryService queryService;

    public SalesStore(InteractiveQueryService queryService) { this.queryService = queryService; }

    public List<ProductSales> allTotals() {
        List<ProductSales> result = new ArrayList<>();
        try (KeyValueIterator<String, ProductSales> it = queryService
                .getQueryableStore(SalesStream.SALES_STORE, QueryableStoreTypes.<String, ProductSales>keyValueStore())
                .all()) {
            it.forEachRemaining(kv -> result.add(kv.value));
        }
        return result;
    }

    // Every 1-minute window that started between from and to
    public List<WindowCount> unitsPerMinute(Instant from, Instant to) {
        List<WindowCount> result = new ArrayList<>();
        try (KeyValueIterator<Windowed<String>, Long> it = queryService
                .getQueryableStore(SalesStream.UNITS_PER_MINUTE_STORE, QueryableStoreTypes.<String, Long>windowStore())
                .fetchAll(from, to)) {
            it.forEachRemaining(kv -> result.add(new WindowCount(Long.valueOf(kv.key.key()),
                    kv.key.window().startTime(), kv.key.window().endTime(), kv.value)));
        }
        return result;
    }
}
