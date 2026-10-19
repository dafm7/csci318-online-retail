package com.csci318.payment.infrastructure.stream;

import com.csci318.payment.infrastructure.analysis.PaymentAnalysisStores;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.stereotype.Component;

// Reads PaymentAnalysisStream's state stores from the running Kafka Streams instance (interactive queries)
@Component
public class InteractivePaymentAnalysisStores implements PaymentAnalysisStores {

    private final InteractiveQueryService queryService;

    public InteractivePaymentAnalysisStores(InteractiveQueryService queryService) { this.queryService = queryService; }

    public ReadOnlyKeyValueStore<String, Long> paymentsPerStatus() {
        return queryService.getQueryableStore(PaymentAnalysisStream.PAYMENTS_PER_STATUS_STORE, QueryableStoreTypes.keyValueStore());
    }

    public ReadOnlyKeyValueStore<String, Double> paidAmount() {
        return queryService.getQueryableStore(PaymentAnalysisStream.PAID_AMOUNT_STORE, QueryableStoreTypes.keyValueStore());
    }

    public ReadOnlyWindowStore<String, Long> updatesPerWindow() {
        return queryService.getQueryableStore(PaymentAnalysisStream.UPDATES_PER_WINDOW_STORE, QueryableStoreTypes.windowStore());
    }
}
