package com.csci318.payment.presentation;

import com.csci318.payment.domain.Payment;
import com.csci318.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @GetMapping
    public List<Payment> all() { return service.all(); }
}
