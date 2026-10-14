package com.csci318.payment.infrastructure.messaging;

import com.csci318.payment.domain.event.OrderPlacedEvent;
import com.csci318.payment.domain.event.PaymentCompletedEvent;
import com.csci318.payment.service.PaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class OrderPlacedEventHandler {

    // in:  order-placed       out: payment-completed   (bindings in application.yml)
    @Bean
    public Function<OrderPlacedEvent, PaymentCompletedEvent> processPayment(PaymentService service) {
        return service::process;
    }
}
