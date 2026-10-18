package com.csci318.order.service;

import com.csci318.order.domain.CustomerOrder;
import com.csci318.order.domain.OrderItem;
import com.csci318.order.domain.OrderStatus;
import com.csci318.order.domain.event.PaymentCompletedEvent;
import com.csci318.order.domain.event.PaymentRefundedEvent;
import com.csci318.order.infrastructure.client.ProductClient;
import com.csci318.order.infrastructure.client.ProductClient.ProductNotFoundException;
import com.csci318.order.infrastructure.client.ProductClient.ProductServiceUnavailableException;
import com.csci318.order.infrastructure.analysis.OrderAnalysisMetrics;
import com.csci318.order.infrastructure.messaging.OrderEventPublisher;
import com.csci318.order.infrastructure.persistence.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

// Use cases for orders; the controller and event handler only pass requests in
@Service
public class OrderService {

    private final OrderRepository repo;
    private final OrderEventPublisher publisher;
    private final ProductClient products;
    private final OrderAnalysisMetrics analysis;

    public OrderService(OrderRepository repo, OrderEventPublisher publisher, ProductClient products, OrderAnalysisMetrics analysis) {
        this.repo = repo;
        this.publisher = publisher;
        this.products = products;
        this.analysis = analysis;
    }

    @Transactional
    public CustomerOrder place(Long customerId, List<OrderLineRequest> requested) {
        if (customerId == null || requested == null || requested.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId and at least one item are required");
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderLineRequest line : requested) {
            if (line == null || line.productId() == null || line.quantity() == null || line.quantity() <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "each item needs productId and positive quantity");
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }
        CustomerOrder order = new CustomerOrder();
        order.setCustomerId(customerId);
        for (var entry : quantities.entrySet()) {
            ProductClient.ProductSnapshot product;
            try { product = products.get(entry.getKey()); }
            catch (ProductNotFoundException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
            catch (ProductServiceUnavailableException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()); }
            if (product.id() == null || product.stock() < 0 || product.price() < 0)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "product service returned invalid product data");
            if (product.stock() < entry.getValue() || "OUT_OF_STOCK".equals(product.availability()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "insufficient stock for product " + entry.getKey());
            if (product.price() < 0) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "product returned an invalid price");
            order.addItem(new OrderItem(product.id(), entry.getValue(), Math.round(product.price() * 100) / 100.0));
        }
        CustomerOrder saved = repo.save(order);
        publisher.orderPlaced(saved);
        publisher.orderAnalysis(saved, "ORDER_CREATED", null);
        return saved;
    }

    @Transactional
    public CustomerOrder cancel(Long id, Long customerId) {
        CustomerOrder order = get(id);
        if (customerId == null || !customerId.equals(order.getCustomerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "order does not belong to this customer");
        String previous = order.getStatus().name();
        try { order.cancel(); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        CustomerOrder saved = repo.save(order);
        publisher.orderCancelled(saved);
        publisher.orderAnalysis(saved, "ORDER_CANCELLED", previous);
        return saved;
    }

    @Transactional
    public CustomerOrder updateStatus(Long id, OrderStatus status) {
        CustomerOrder order = get(id);
        String previous = order.getStatus().name();
        try { order.updateStatus(status); }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()); }
        CustomerOrder saved = repo.save(order);
        publisher.orderAnalysis(saved, "ORDER_STATUS_CHANGED", previous);
        return saved;
    }

    public List<CustomerOrder> find(Long customerId) {
        return (customerId == null) ? repo.findAll() : repo.findByCustomerId(customerId);
    }

    public Map<String, Object> analysis() {
        return analysis.snapshot();
    }

    public CustomerOrder get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    // Called for every PaymentCompletedEvent: mark the order PAID or FAILED
    @Transactional
    public void recordPayment(PaymentCompletedEvent event) {
        repo.findById(event.orderId()).ifPresent(order -> {
            if (!order.awaitingPayment()) {   // Kafka can deliver an event twice; ignore the duplicate
                System.out.println("[order-service] order " + order.getId() + " already " + order.getStatus() + ", ignoring duplicate payment");
                return;
            }
            order.recordPayment(event.success());
            repo.save(order);
            publisher.orderAnalysis(order, "ORDER_STATUS_CHANGED", OrderStatus.PENDING.name());
            System.out.println("[order-service] order " + order.getId() + " -> " + order.getStatus());
        });
    }
    @Transactional
    public void recordRefund(PaymentRefundedEvent event) {
        repo.findById(event.orderId()).ifPresent(order -> {
            if (order.getRefundedAt() != null) return;
            String previous = order.getStatus().name();
            try { order.markRefunded(); repo.save(order); publisher.orderAnalysis(order, "REFUND_COMPLETED", previous); }
            catch (IllegalStateException ex) { System.out.println("[order-service] refund event ignored: " + ex.getMessage()); }
        });
    }
    public record OrderLineRequest(Long productId, Integer quantity) {}
}
