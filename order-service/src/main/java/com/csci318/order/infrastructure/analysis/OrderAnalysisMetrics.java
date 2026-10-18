package com.csci318.order.infrastructure.analysis;

import com.csci318.order.domain.event.OrderAnalysisEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Component
public class OrderAnalysisMetrics {
    private final OrderAnalysisRecordRepository records;

    public OrderAnalysisMetrics(OrderAnalysisRecordRepository records) { this.records = records; }

    @Transactional
    public void accept(OrderAnalysisEvent event) {
        if (event == null || event.eventId() == null || event.orderId() == null
                || event.eventType() == null || event.status() == null || event.occurredAt() == null) return;
        if (records.existsById(event.eventId())) return;
        records.save(new OrderAnalysisRecord(event));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot() {
        List<OrderAnalysisRecord> all = records.findAllByOrderByOccurredAtAscEventIdAsc();
        Map<Long, String> currentStatuses = new HashMap<>();
        Set<Long> createdOrders = new HashSet<>();
        Set<Long> cancelledOrders = new HashSet<>();
        for (OrderAnalysisRecord event : all) {
            if ("ORDER_CREATED".equals(event.getEventType())) createdOrders.add(event.getOrderId());
            if ("ORDER_CANCELLED".equals(event.getEventType())) cancelledOrders.add(event.getOrderId());
            currentStatuses.put(event.getOrderId(), event.getStatus());
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        currentStatuses.values().forEach(status -> counts.merge(status, 1L, Long::sum));

        Instant cutoff = Instant.now().minusSeconds(60);
        List<OrderAnalysisRecord> recent = records.findAllByOccurredAtAfter(cutoff);
        long createdLastMinute = recent.stream().filter(e -> "ORDER_CREATED".equals(e.getEventType())).count();
        long cancelledLastMinute = recent.stream().filter(e -> "ORDER_CANCELLED".equals(e.getEventType())).count();
        long changesLastMinute = recent.stream().filter(e -> !"ORDER_CREATED".equals(e.getEventType())).count();
        return Map.of("totalOrders", (long) createdOrders.size(), "statusCounts", counts,
                "cancellationRate", createdOrders.isEmpty() ? 0.0 : (double) cancelledOrders.size() / createdOrders.size(),
                "ordersLastMinute", createdLastMinute, "cancelledLastMinute", cancelledLastMinute,
                "statusChangesLastMinute", changesLastMinute);
    }
}
