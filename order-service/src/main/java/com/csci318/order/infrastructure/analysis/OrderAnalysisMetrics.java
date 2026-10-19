package com.csci318.order.infrastructure.analysis;

import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.csci318.order.infrastructure.stream.OrderAnalysisStream.*;

// Order analysis summary, read from the Kafka Streams state stores (updated as each order event arrives)
@Component
public class OrderAnalysisMetrics {

    private final OrderAnalysisStores stores;

    public OrderAnalysisMetrics(OrderAnalysisStores stores) { this.stores = stores; }

    public Map<String, Object> snapshot() { return snapshot(Instant.now()); }

    public Map<String, Object> snapshot(Instant now) {
        try {
            Map<String, Long> counts = new LinkedHashMap<>();
            try (KeyValueIterator<String, Long> it = stores.ordersPerStatus().all()) {
                it.forEachRemaining(kv -> { if (kv.value != null && kv.value > 0) counts.put(kv.key, kv.value); });
            }
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            // A cancelled order stays CANCELLED (even after a refund), so this counts every cancelled order
            long cancelled = counts.getOrDefault("CANCELLED", 0L);
            long windowStart = lastMinuteWindowStart(now);
            return Map.of("totalOrders", total, "statusCounts", counts,
                    "cancellationRate", total == 0 ? 0.0 : (double) cancelled / total,
                    "ordersLastMinute", countInWindow(CREATED, windowStart),
                    "cancelledLastMinute", countInWindow(CANCELLED, windowStart),
                    "statusChangesLastMinute", countInWindow(STATUS_CHANGE, windowStart));
        } catch (InvalidStateStoreException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "order analysis stream is starting, try again in a few seconds");
        }
    }

    // The hopping window that covers the most of the last 60 seconds: the oldest window that is still open
    static long lastMinuteWindowStart(Instant now) {
        long size = WINDOW_SIZE.toMillis(), advance = WINDOW_ADVANCE.toMillis();
        return Math.floorDiv(now.toEpochMilli() - size, advance) * advance + advance;
    }

    private long countInWindow(String key, long windowStart) {
        long total = 0;
        try (WindowStoreIterator<Long> it = stores.eventsPerWindow().fetch(key, Instant.ofEpochMilli(windowStart), Instant.ofEpochMilli(windowStart))) {
            while (it.hasNext()) total += it.next().value;
        }
        return total;
    }
}
