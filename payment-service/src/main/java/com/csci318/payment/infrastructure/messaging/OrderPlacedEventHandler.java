package com.csci318.payment.infrastructure.messaging;

import com.csci318.payment.domain.event.OrderPlacedEvent;
import com.csci318.payment.service.PaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class OrderPlacedEventHandler {

    // Order creation registers a pending payment; the customer submits payment separately.
    @Bean
    public Consumer<OrderPlacedEvent> registerPayment(PaymentService service) {
        return service::registerOrder;
    }
}
