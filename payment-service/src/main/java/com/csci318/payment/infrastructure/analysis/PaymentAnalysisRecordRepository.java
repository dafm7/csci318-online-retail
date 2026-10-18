package com.csci318.payment.infrastructure.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface PaymentAnalysisRecordRepository extends JpaRepository<PaymentAnalysisRecord, String> {
    List<PaymentAnalysisRecord> findAllByOrderByOccurredAtAscEventIdAsc();
    List<PaymentAnalysisRecord> findAllByOccurredAtAfter(Instant cutoff);
}
