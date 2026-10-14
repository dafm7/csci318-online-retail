package com.csci318.payment.infrastructure.persistence;

import com.csci318.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {}
