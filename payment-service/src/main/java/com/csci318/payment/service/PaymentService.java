package com.csci318.payment.service;

import com.csci318.payment.domain.Payment;
import com.csci318.payment.domain.PaymentStatus;
import com.csci318.payment.domain.Refund;
import com.csci318.payment.domain.event.OrderPlacedEvent;
import com.csci318.payment.domain.event.PaymentCompletedEvent;
import com.csci318.payment.domain.event.PaymentRefundedEvent;
import com.csci318.payment.infrastructure.client.OrderClient;
import com.csci318.payment.infrastructure.client.OrderClient.OrderNotFoundException;
import com.csci318.payment.infrastructure.client.OrderClient.OrderServiceUnavailableException;
import com.csci318.payment.infrastructure.analysis.PaymentAnalysisMetrics;
import com.csci318.payment.infrastructure.persistence.PaymentRepository;
import com.csci318.payment.infrastructure.persistence.RefundRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

// import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.csci318.payment.infrastructure.outbox.Outbox;

@Service
public class PaymentService {
    private final PaymentRepository repo;
    private final RefundRepository refunds;
    private final OrderClient orders;
    private final Outbox outbox;
    private final PaymentAnalysisMetrics analysis;

    public PaymentService(PaymentRepository repo, RefundRepository refunds, OrderClient orders, Outbox outbox, PaymentAnalysisMetrics analysis) {
        this.repo = repo; this.refunds = refunds; this.orders = orders; this.outbox = outbox; this.analysis = analysis;
    }

    @Transactional
    public void registerOrder(OrderPlacedEvent event) {
        if (event == null || event.orderId() == null || event.amount() == null || event.amount().signum() < 0)
            throw new IllegalArgumentException("order event needs an order id and non-negative amount");
        if (repo.findByOrderId(event.orderId()).isPresent()) return;
        publishAnalysis(repo.save(Payment.pending(event.orderId(), event.amount())));
    }

    @Transactional
    public Payment pay(Long orderId, String paymentMethod) {
        if (orderId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
        if (paymentMethod == null || paymentMethod.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentMethod is required (mock payment accepts a method label)");
        OrderClient.OrderSnapshot order;
        try { order = orders.get(orderId); }
        catch (OrderNotFoundException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); }
        catch (OrderServiceUnavailableException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()); }
        if (order.amount() == null || order.status() == null)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "order service returned invalid order data");
        if (!"PENDING".equals(order.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "only a pending order can be paid");
        Payment payment = repo.findByOrderId(orderId).orElseGet(() -> Payment.pending(orderId, order.amount()));
        if (payment.getStatus() != PaymentStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "payment has already been processed");
        payment.complete();
        Payment saved = repo.save(payment);
        outbox.enqueue("paymentCompleted-out-0", new PaymentCompletedEvent(orderId, saved.isSuccess()));
        publishAnalysis(saved);
        return saved;
    }

    @Transactional
    public Refund refund(Long orderId) {
        if (orderId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
        Payment payment = repo.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "payment not found"));
        Refund existing = refunds.findByPaymentId(payment.getId()).orElse(null);
        if (existing != null) return existing;
        OrderClient.OrderSnapshot order;
        try { order = orders.get(orderId); }
        catch (OrderNotFoundException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); }
        catch (OrderServiceUnavailableException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()); }
        if (order.status() == null)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "order service returned invalid order data");
        if (!"PAID".equals(order.status()) && !"CANCELLED".equals(order.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "only a paid or cancelled order with a successful payment can be refunded");
        try { payment.refund(); }
        catch (IllegalStateException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage()); }
        repo.save(payment);
        Refund refund = refunds.save(Refund.completed(payment));
        outbox.enqueue("paymentRefunded-out-0", new PaymentRefundedEvent(orderId));
        publishAnalysis(payment);
        return refund;
    }

    public List<Payment> all() { return repo.findAll(); }
    public List<Refund> allRefunds() { return refunds.findAll(); }
    public Map<String, Object> analysis() {
        return analysis.snapshot();
    }

    private void publishAnalysis(Payment payment) {
        outbox.enqueue("paymentAnalysis-out-0", new com.csci318.payment.domain.event.PaymentAnalysisEvent(
                java.util.UUID.randomUUID().toString(), payment.getOrderId(), payment.getStatus().name(), payment.getAmount(), java.time.Instant.now()));
    }
}
