package com.csci318.order.infrastructure.messaging;

import com.csci318.order.domain.CustomerOrder;
import com.csci318.order.domain.event.OrderLineEvent;
import com.csci318.order.domain.event.OrderAnalysisEvent;
import com.csci318.order.domain.event.OrderPlacedEvent;
import com.csci318.order.domain.event.OrderCancelledEvent;
import com.csci318.order.infrastructure.outbox.Outbox;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

// Publishes OrderPlacedEvent to the "order-placed" topic (consumed by payment-service and product-service)
@Component
public class OrderEventPublisher {

    private final Outbox outbox;

    public OrderEventPublisher(Outbox outbox) { this.outbox = outbox; }

    public void orderPlaced(CustomerOrder o) {
        var items = o.getItems().stream().map(i -> new OrderLineEvent(i.getProductId(), i.getQuantity(), i.getUnitPrice())).toList();
        outbox.enqueue("orderPlaced-out-0", new OrderPlacedEvent(
                o.getId(), o.getCustomerId(), o.getProductId(), o.getQuantity(), o.getAmount(), items));
    }

    public void orderCancelled(CustomerOrder o) {
        var items = o.getItems().stream().map(i -> new OrderLineEvent(i.getProductId(), i.getQuantity(), i.getUnitPrice())).toList();
        outbox.enqueue("orderCancelled-out-0", new OrderCancelledEvent(o.getId(), o.getProductId(), o.getQuantity(), items));
    }

    public void orderAnalysis(CustomerOrder order, String eventType, String previousStatus) {
        outbox.enqueue("orderAnalysis-out-0", new OrderAnalysisEvent(UUID.randomUUID().toString(),
                order.getId(), eventType, previousStatus, order.getStatus().name(), Instant.now()));
    }
}
