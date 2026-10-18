package com.csci318.payment.service;

import com.csci318.payment.domain.Payment;
import com.csci318.payment.domain.PaymentStatus;
import com.csci318.payment.domain.Refund;
import com.csci318.payment.domain.event.OrderPlacedEvent;
import com.csci318.payment.infrastructure.analysis.PaymentAnalysisMetrics;
import com.csci318.payment.infrastructure.client.OrderClient;
import com.csci318.payment.infrastructure.outbox.Outbox;
import com.csci318.payment.infrastructure.persistence.PaymentRepository;
import com.csci318.payment.infrastructure.persistence.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private PaymentRepository payments;
    private RefundRepository refunds;
    private OrderClient orders;
    private Outbox outbox;
    private PaymentService service;

    @BeforeEach void setUp() {
        payments = mock(PaymentRepository.class);
        refunds = mock(RefundRepository.class);
        orders = mock(OrderClient.class);
        outbox = mock(Outbox.class);
        service = new PaymentService(payments, refunds, orders, outbox, mock(PaymentAnalysisMetrics.class));
        when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
        when(refunds.save(any(Refund.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void orderEventCreatesOnePendingPaymentAndDuplicateIsIgnored() {
        OrderPlacedEvent event = new OrderPlacedEvent(9L, 2L, 1L, 1, 29.99);
        when(payments.findByOrderId(9L)).thenReturn(Optional.empty()).thenReturn(Optional.of(Payment.pending(9L, event.amount())));
        // when(payments.findByOrderId(9L)).thenReturn(Optional.empty(), Optional.of(Payment.pending(9L, event.amount())));
        service.registerOrder(event);
        service.registerOrder(event);
        verify(payments, times(1)).save(any(Payment.class));
    }

    @Test void payUsesOrderAmountAndQueuesCompletionAndAnalysisEvents() {
        when(orders.get(9L)).thenReturn(new OrderClient.OrderSnapshot(9L, 29.99, "PENDING"));
        when(payments.findByOrderId(9L)).thenReturn(Optional.of(Payment.pending(9L, 29.99)));

        Payment payment = service.pay(9L, "mock-card");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertTrue(payment.isSuccess());
        assertEquals(29.99, payment.getAmount(), 0.001);
        verify(outbox).enqueue(eq("paymentCompleted-out-0"), any());
        verify(outbox).enqueue(eq("paymentAnalysis-out-0"), any());
    }

    @Test void paymentAboveMockLimitFailsAndCannotBeRefunded() {
        when(orders.get(9L)).thenReturn(new OrderClient.OrderSnapshot(9L, 1000.01, "PENDING"));
        when(payments.findByOrderId(9L)).thenReturn(Optional.of(Payment.pending(9L, 1000.01)));
        Payment payment = service.pay(9L, "mock-card");
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        when(payments.findByOrderId(9L)).thenReturn(Optional.of(payment));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class, () -> service.refund(9L)).getStatusCode());
    }

    @Test void duplicatePaymentAndInvalidPaymentRequestAreRejected() {
        when(orders.get(9L)).thenReturn(new OrderClient.OrderSnapshot(9L, 29.99, "PENDING"));
        when(payments.findByOrderId(9L)).thenReturn(Optional.of(Payment.pending(9L, 29.99)));
        service.pay(9L, "mock-card");
        Payment paid = payments.findByOrderId(9L).orElseThrow();
        paid.complete();
        when(payments.findByOrderId(9L)).thenReturn(Optional.of(paid));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.pay(9L, "mock-card")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.pay(9L, " ")).getStatusCode());
    }

    @Test void refundCreatesOneRefundAndQueuesOrderNotification() {
        Payment paid = Payment.pending(9L, 29.99);
        paid.complete();
        when(payments.findByOrderId(9L)).thenReturn(Optional.of(paid));
        when(refunds.findByPaymentId(any())).thenReturn(Optional.empty());
        when(orders.get(9L)).thenReturn(new OrderClient.OrderSnapshot(9L, 29.99, "PAID"));

        service.refund(9L);

        assertEquals(PaymentStatus.REFUNDED, paid.getStatus());
        verify(refunds).save(any(Refund.class));
        verify(outbox).enqueue(eq("paymentRefunded-out-0"), any());
        verify(outbox).enqueue(eq("paymentAnalysis-out-0"), any());
    }
}
