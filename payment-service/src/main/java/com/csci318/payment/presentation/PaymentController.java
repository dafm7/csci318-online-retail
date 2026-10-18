package com.csci318.payment.presentation;

import com.csci318.payment.domain.Payment;
import com.csci318.payment.domain.Refund;
import com.csci318.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @GetMapping
    public List<Payment> all() { return service.all(); }

    @PostMapping
    public Payment pay(@RequestBody PaymentRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        return service.pay(request.orderId(), request.paymentMethod());
    }

    @GetMapping("/analysis/summary")
    public Map<String, Object> analysis() { return service.analysis(); }

    @PostMapping("/orders/{orderId}/refund")
    public Refund refund(@PathVariable Long orderId) { return service.refund(orderId); }

    @GetMapping("/refunds")
    public List<Refund> refunds() { return service.allRefunds(); }

    public record PaymentRequest(Long orderId, String paymentMethod) {}
}
