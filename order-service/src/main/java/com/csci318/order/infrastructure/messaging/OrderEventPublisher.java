package com.csci318.order.infrastructure.messaging;

import com.csci318.order.domain.CustomerOrder;
import com.csci318.order.domain.event.OrderPlacedEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

// Publishes OrderPlacedEvent to the "order-placed" topic (consumed by payment-service and product-service)
@Component
public class OrderEventPublisher {

    private final StreamBridge streamBridge;

    public OrderEventPublisher(StreamBridge streamBridge) { this.streamBridge = streamBridge; }

    public void orderPlaced(CustomerOrder o) {
        streamBridge.send("orderPlaced-out-0", new OrderPlacedEvent(
                o.getId(), o.getCustomerId(), o.getProductId(), o.getQuantity(), o.getAmount()));
    }
}
