package com.csci318.payment.infrastructure.persistence;

import com.csci318.payment.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByPaymentId(Long paymentId);
}
