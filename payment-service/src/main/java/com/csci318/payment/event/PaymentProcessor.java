package com.csci318.payment.event;

import com.csci318.payment.model.Payment;
import com.csci318.payment.repository.PaymentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class PaymentProcessor {

    // in:  order-placed       out: payment-completed
    // Mock payment: anything up to $1000 succeeds. Replace with real logic.
    @Bean
    public Function<OrderPlacedEvent, PaymentCompletedEvent> processPayment(PaymentRepository repo) {
        return event -> {
            boolean ok = event.amount() <= 1000;
            repo.save(new Payment(event.orderId(), event.amount(), ok));
            System.out.println("[payment-service] order " + event.orderId() + " paid=" + ok);
            return new PaymentCompletedEvent(event.orderId(), ok);
        };
    }
}
