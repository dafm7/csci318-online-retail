package com.csci318.order.service;

import com.csci318.order.domain.CustomerOrder;
import com.csci318.order.domain.event.PaymentCompletedEvent;
import com.csci318.order.infrastructure.messaging.OrderEventPublisher;
import com.csci318.order.infrastructure.persistence.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Use cases for orders; the controller and event handler only pass requests in
@Service
public class OrderService {

    private final OrderRepository repo;
    private final OrderEventPublisher publisher;

    public OrderService(OrderRepository repo, OrderEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    // Save the order, then publish an OrderPlaced event to Kafka
    public CustomerOrder place(CustomerOrder order) {
        if (order.getCustomerId() == null || order.getProductId() == null || order.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId, productId and a positive quantity are required");
        }
        CustomerOrder saved = repo.save(order);
        publisher.orderPlaced(saved);
        return saved;
    }

    public List<CustomerOrder> find(Long customerId) {
        return (customerId == null) ? repo.findAll() : repo.findByCustomerId(customerId);
    }

    public CustomerOrder get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    // Called for every PaymentCompletedEvent: mark the order PAID or FAILED
    public void recordPayment(PaymentCompletedEvent event) {
        repo.findById(event.orderId()).ifPresent(order -> {
            if (!order.awaitingPayment()) {   // Kafka can deliver an event twice; ignore the duplicate
                System.out.println("[order-service] order " + order.getId() + " already " + order.getStatus() + ", ignoring duplicate payment");
                return;
            }
            order.recordPayment(event.success());
            repo.save(order);
            System.out.println("[order-service] order " + order.getId() + " -> " + order.getStatus());
        });
    }
}
