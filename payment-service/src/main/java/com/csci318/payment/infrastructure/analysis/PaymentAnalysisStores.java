package com.csci318.payment.infrastructure.analysis;

import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;

// Read access to the state stores built by PaymentAnalysisStream
public interface PaymentAnalysisStores {
    ReadOnlyKeyValueStore<String, Long> paymentsPerStatus();
    ReadOnlyKeyValueStore<String, Double> paidAmount();
    ReadOnlyWindowStore<String, Long> updatesPerWindow();
}
