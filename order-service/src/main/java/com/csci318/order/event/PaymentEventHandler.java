package com.csci318.order.event;

import com.csci318.order.repository.OrderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class PaymentEventHandler {

    // Listens on "payment-completed" and updates the order status
    @Bean
    public Consumer<PaymentCompletedEvent> paymentCompleted(OrderRepository repo) {
        return event -> repo.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(event.success() ? "PAID" : "FAILED");
            repo.save(order);
            System.out.println("[order-service] order " + order.getId() + " -> " + order.getStatus());
        });
    }
}
