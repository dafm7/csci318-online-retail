package com.csci318.payment.service;

import com.csci318.payment.domain.Payment;
import com.csci318.payment.domain.event.OrderPlacedEvent;
import com.csci318.payment.domain.event.PaymentCompletedEvent;
import com.csci318.payment.infrastructure.persistence.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Use cases for payments; the controller and event handler only pass requests in
@Service
public class PaymentService {

    private final PaymentRepository repo;

    public PaymentService(PaymentRepository repo) { this.repo = repo; }

    // Called for every OrderPlacedEvent: try the payment and report the result
    public PaymentCompletedEvent process(OrderPlacedEvent event) {
        Payment payment = repo.save(Payment.attempt(event.orderId(), event.amount()));
        System.out.println("[payment-service] order " + event.orderId() + " paid=" + payment.isSuccess());
        return new PaymentCompletedEvent(event.orderId(), payment.isSuccess());
    }

    public List<Payment> all() { return repo.findAll(); }
}
