package com.csci318.product.event;

import com.csci318.product.model.Product;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class StockEventPublisher {

    private final StreamBridge streamBridge;

    public StockEventPublisher(StreamBridge streamBridge) { this.streamBridge = streamBridge; }

    public void stockChanged(Product p) {
        streamBridge.send("stockChanged-out-0", new StockChangedEvent(p.getId(), p.getName(), p.getStock()));
    }
}
