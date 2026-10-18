package com.csci318.order.infrastructure.analysis;

import com.csci318.order.domain.event.OrderAnalysisEvent;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderAnalysisMetricsTest {
    @Test void snapshotUsesPersistedStatusesAndFiltersWindowByQueryTime() {
        OrderAnalysisRecord created = new OrderAnalysisRecord(new OrderAnalysisEvent("1", 7L, "ORDER_CREATED", null, "PENDING", Instant.now()));
        OrderAnalysisRecord paid = new OrderAnalysisRecord(new OrderAnalysisEvent("2", 7L, "ORDER_STATUS_CHANGED", "PENDING", "PAID", Instant.now()));
        OrderAnalysisRecord cancelled = new OrderAnalysisRecord(new OrderAnalysisEvent("3", 7L, "ORDER_CANCELLED", "PAID", "CANCELLED", Instant.now()));
        OrderAnalysisRecordRepository repository = mock(OrderAnalysisRecordRepository.class);
        when(repository.findAllByOrderByOccurredAtAscEventIdAsc()).thenReturn(List.of(created, paid, cancelled));
        when(repository.findAllByOccurredAtAfter(any())).thenReturn(List.of());

        Map<String, Object> result = new OrderAnalysisMetrics(repository).snapshot();

        assertEquals(1L, result.get("totalOrders"));
        assertEquals(1.0, result.get("cancellationRate"));
        assertEquals(0L, result.get("ordersLastMinute"));
        assertEquals(Map.of("CANCELLED", 1L), result.get("statusCounts"));
    }

    @Test void acceptDeduplicatesKafkaRedeliveryByEventId() {
        OrderAnalysisRecordRepository repository = mock(OrderAnalysisRecordRepository.class);
        when(repository.existsById("same-id")).thenReturn(true);
        new OrderAnalysisMetrics(repository).accept(new OrderAnalysisEvent("same-id", 7L, "ORDER_CREATED", null, "PENDING", Instant.now()));
        verify(repository, never()).save(any());
    }
}
