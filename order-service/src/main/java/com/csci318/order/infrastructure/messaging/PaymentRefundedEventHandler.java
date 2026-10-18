package com.csci318.order.infrastructure.messaging;

import com.csci318.order.domain.event.PaymentRefundedEvent;
import com.csci318.order.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Consumer;

@Configuration
public class PaymentRefundedEventHandler {
    @Bean
    public Consumer<PaymentRefundedEvent> paymentRefunded(OrderService service) { return service::recordRefund; }
}
