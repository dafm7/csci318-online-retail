package com.csci318.payment.infrastructure.analysis;

import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.csci318.payment.infrastructure.stream.PaymentAnalysisStream.*;

// Payment analysis summary, read from the Kafka Streams state stores (updated as each payment event arrives)
@Component
public class PaymentAnalysisMetrics {

    private final PaymentAnalysisStores stores;

    public PaymentAnalysisMetrics(PaymentAnalysisStores stores) { this.stores = stores; }

    public Map<String, Object> snapshot() { return snapshot(Instant.now()); }

    public Map<String, Object> snapshot(Instant now) {
        try {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (String status : List.of("PENDING", "SUCCESS", "FAILED", "REFUNDED")) counts.put(status, 0L);
            try (KeyValueIterator<String, Long> it = stores.paymentsPerStatus().all()) {
                it.forEachRemaining(kv -> { if (kv.value != null) counts.put(kv.key, kv.value); });
            }
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            long successful = counts.get("SUCCESS") + counts.get("REFUNDED");
            long completed = successful + counts.get("FAILED");
            Double paid = stores.paidAmount().get(TOTAL);
            return Map.of("totalPayments", total, "statusCounts", counts,
                    "successRate", completed == 0 ? 0.0 : (double) successful / completed,
                    "paymentUpdatesLastMinute", updatesInWindow(lastMinuteWindowStart(now)),
                    "totalAmount", paid == null ? 0.0 : paid);
        } catch (InvalidStateStoreException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "payment analysis stream is starting, try again in a few seconds");
        }
    }

    // The hopping window that covers the most of the last 60 seconds: the oldest window that is still open
    static long lastMinuteWindowStart(Instant now) {
        long size = WINDOW_SIZE.toMillis(), advance = WINDOW_ADVANCE.toMillis();
        return Math.floorDiv(now.toEpochMilli() - size, advance) * advance + advance;
    }

    private long updatesInWindow(long windowStart) {
        long total = 0;
        try (WindowStoreIterator<Long> it = stores.updatesPerWindow().fetch(TOTAL, Instant.ofEpochMilli(windowStart), Instant.ofEpochMilli(windowStart))) {
            while (it.hasNext()) total += it.next().value;
        }
        return total;
    }
}
