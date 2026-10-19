package com.csci318.order.infrastructure.analysis;

import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;

// Read access to the state stores built by OrderAnalysisStream
public interface OrderAnalysisStores {
    ReadOnlyKeyValueStore<String, Long> ordersPerStatus();
    ReadOnlyWindowStore<String, Long> eventsPerWindow();
}
