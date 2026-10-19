package com.csci318.order.infrastructure.stream;

import com.csci318.order.infrastructure.analysis.OrderAnalysisStores;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.stereotype.Component;

// Reads OrderAnalysisStream's state stores from the running Kafka Streams instance (interactive queries)
@Component
public class InteractiveOrderAnalysisStores implements OrderAnalysisStores {

    private final InteractiveQueryService queryService;

    public InteractiveOrderAnalysisStores(InteractiveQueryService queryService) { this.queryService = queryService; }

    public ReadOnlyKeyValueStore<String, Long> ordersPerStatus() {
        return queryService.getQueryableStore(OrderAnalysisStream.ORDERS_PER_STATUS_STORE, QueryableStoreTypes.keyValueStore());
    }

    public ReadOnlyWindowStore<String, Long> eventsPerWindow() {
        return queryService.getQueryableStore(OrderAnalysisStream.EVENTS_PER_WINDOW_STORE, QueryableStoreTypes.windowStore());
    }
}
