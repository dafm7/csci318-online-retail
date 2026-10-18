package com.csci318.payment.infrastructure.analysis;

import com.csci318.payment.domain.event.PaymentAnalysisEvent;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentAnalysisMetricsTest {
    @Test void successRateExcludesPendingTransactionsAndUsesLatestState() {
        PaymentAnalysisRecord pending = new PaymentAnalysisRecord(new PaymentAnalysisEvent("1", 1L, "PENDING", 25.00, Instant.now()));
        PaymentAnalysisRecord success = new PaymentAnalysisRecord(new PaymentAnalysisEvent("2", 1L, "SUCCESS", 25.00, Instant.now()));
        PaymentAnalysisRecord failed = new PaymentAnalysisRecord(new PaymentAnalysisEvent("3", 2L, "FAILED", 1200.00, Instant.now()));
        PaymentAnalysisRecord waiting = new PaymentAnalysisRecord(new PaymentAnalysisEvent("4", 3L, "PENDING", 10.00, Instant.now()));
        PaymentAnalysisRecordRepository repository = mock(PaymentAnalysisRecordRepository.class);
        when(repository.findAllByOrderByOccurredAtAscEventIdAsc()).thenReturn(List.of(pending, success, failed, waiting));
        when(repository.findAllByOccurredAtAfter(any())).thenReturn(List.of());

        Map<String, Object> result = new PaymentAnalysisMetrics(repository).snapshot();

        assertEquals(3L, result.get("totalPayments"));
        assertEquals(0.5, result.get("successRate"));
        assertEquals(0L, result.get("paymentUpdatesLastMinute"));
        assertEquals(25.00, (double) result.get("totalAmount"), 0.001);
        assertEquals(Map.of("PENDING", 1L, "SUCCESS", 1L, "FAILED", 1L, "REFUNDED", 0L), result.get("statusCounts"));
    }

    @Test void acceptDeduplicatesKafkaRedeliveryByEventId() {
        PaymentAnalysisRecordRepository repository = mock(PaymentAnalysisRecordRepository.class);
        when(repository.existsById("same-id")).thenReturn(true);
        new PaymentAnalysisMetrics(repository).accept(new PaymentAnalysisEvent("same-id", 1L, "SUCCESS", 4.00, Instant.now()));
        verify(repository, never()).save(any());
    }
}
