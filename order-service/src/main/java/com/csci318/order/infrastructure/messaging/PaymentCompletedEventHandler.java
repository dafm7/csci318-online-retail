package com.csci318.order.infrastructure.messaging;

import com.csci318.order.domain.event.PaymentCompletedEvent;
import com.csci318.order.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class PaymentCompletedEventHandler {

    // Bound to the "payment-completed" topic in application.yml (paymentCompleted-in-0)
    @Bean
    public Consumer<PaymentCompletedEvent> paymentCompleted(OrderService service) {
        return service::recordPayment;
    }
}
