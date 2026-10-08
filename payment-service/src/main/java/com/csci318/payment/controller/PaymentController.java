package com.csci318.payment.controller;

import com.csci318.payment.model.Payment;
import com.csci318.payment.repository.PaymentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin
public class PaymentController {
    private final PaymentRepository repo;

    public PaymentController(PaymentRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Payment> all() { return repo.findAll(); }
}
