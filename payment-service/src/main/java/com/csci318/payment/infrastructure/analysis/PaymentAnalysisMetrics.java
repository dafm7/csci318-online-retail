package com.csci318.payment.infrastructure.analysis;

import com.csci318.payment.domain.event.PaymentAnalysisEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Component
public class PaymentAnalysisMetrics {
    private final PaymentAnalysisRecordRepository records;

    public PaymentAnalysisMetrics(PaymentAnalysisRecordRepository records) { this.records = records; }

    @Transactional
    public void accept(PaymentAnalysisEvent event) {
        if (event == null || event.eventId() == null || event.orderId() == null || event.status() == null
                || event.amount() == null || event.occurredAt() == null) return;
        if (records.existsById(event.eventId())) return;
        records.save(new PaymentAnalysisRecord(event));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : List.of("PENDING", "SUCCESS", "FAILED", "REFUNDED")) counts.put(status, 0L);
        Map<Long, PaymentAnalysisRecord> current = new HashMap<>();
        for (PaymentAnalysisRecord event : records.findAllByOrderByOccurredAtAscEventIdAsc()) current.put(event.getOrderId(), event);
        current.values().forEach(event -> counts.merge(event.getStatus(), 1L, Long::sum));
        long successful = counts.get("SUCCESS") + counts.get("REFUNDED");
        long completed = successful + counts.get("FAILED");
        BigDecimal amount = current.values().stream()
                .filter(e -> "SUCCESS".equals(e.getStatus()) || "REFUNDED".equals(e.getStatus()))
                .map(PaymentAnalysisRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant cutoff = Instant.now().minusSeconds(60);
        long updatesLastMinute = records.findAllByOccurredAtAfter(cutoff).size();
        return Map.of("totalPayments", (long) current.size(), "statusCounts", counts,
                "successRate", completed == 0 ? 0.0 : (double) successful / completed,
                "paymentUpdatesLastMinute", updatesLastMinute, "totalAmount", amount);
    }
}
