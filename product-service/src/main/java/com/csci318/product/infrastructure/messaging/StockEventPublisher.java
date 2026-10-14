package com.csci318.product.infrastructure.messaging;

import com.csci318.product.domain.Product;
import com.csci318.product.domain.event.StockChangedEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

// Publishes StockChangedEvent to the "stock-changed" topic (consumed by inventory-service)
@Component
public class StockEventPublisher {

    private final StreamBridge streamBridge;

    public StockEventPublisher(StreamBridge streamBridge) { this.streamBridge = streamBridge; }

    public void stockChanged(Product p) {
        streamBridge.send("stockChanged-out-0", new StockChangedEvent(p.getId(), p.getName(), p.getStock()));
    }
}
