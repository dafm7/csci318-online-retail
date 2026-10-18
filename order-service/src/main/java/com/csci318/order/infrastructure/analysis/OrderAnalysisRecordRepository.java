package com.csci318.order.infrastructure.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface OrderAnalysisRecordRepository extends JpaRepository<OrderAnalysisRecord, String> {
    List<OrderAnalysisRecord> findAllByOrderByOccurredAtAscEventIdAsc();
    List<OrderAnalysisRecord> findAllByOccurredAtAfter(Instant cutoff);
}
