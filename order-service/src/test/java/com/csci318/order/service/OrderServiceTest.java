package com.csci318.order.service;

import com.csci318.order.domain.CustomerOrder;
import com.csci318.order.domain.OrderStatus;
import com.csci318.order.domain.event.PaymentCompletedEvent;
import com.csci318.order.infrastructure.analysis.OrderAnalysisMetrics;
import com.csci318.order.infrastructure.client.ProductClient;
import com.csci318.order.infrastructure.messaging.OrderEventPublisher;
import com.csci318.order.infrastructure.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private OrderRepository repository;
    private OrderEventPublisher publisher;
    private ProductClient products;
    private OrderService service;

    @BeforeEach void setUp() {
        repository = mock(OrderRepository.class);
        publisher = mock(OrderEventPublisher.class);
        products = mock(ProductClient.class);
        service = new OrderService(repository, publisher, products, mock(OrderAnalysisMetrics.class));
        when(repository.save(any(CustomerOrder.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void placeUsesServerPricesAndKeepsPreciseMultiItemTotal() {
        when(products.get(1L)).thenReturn(new ProductClient.ProductSnapshot(1L, 29.99, 10, "IN_STOCK"));
        when(products.get(4L)).thenReturn(new ProductClient.ProductSnapshot(4L, 12.00, 10, "IN_STOCK"));

        CustomerOrder order = service.place(2L, List.of(
                new OrderService.OrderLineRequest(1L, 2), new OrderService.OrderLineRequest(4L, 1)));

        assertEquals(71.98, order.getAmount(), 0.001);
        assertEquals(3, order.getQuantity());
        assertEquals(2, order.getItems().size());
        verify(publisher).orderPlaced(order);
        verify(publisher).orderAnalysis(order, "ORDER_CREATED", null);
    }

    @Test void placeRejectsNullOrNonPositiveLineQuantityAsBadRequest() {
        ResponseStatusException nullQuantity = assertThrows(ResponseStatusException.class,
                () -> service.place(2L, List.of(new OrderService.OrderLineRequest(1L, null))));
        ResponseStatusException zeroQuantity = assertThrows(ResponseStatusException.class,
                () -> service.place(2L, List.of(new OrderService.OrderLineRequest(1L, 0))));
        assertEquals(HttpStatus.BAD_REQUEST, nullQuantity.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, zeroQuantity.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test void placeRejectsInsufficientStockWithoutSavingOrder() {
        when(products.get(1L)).thenReturn(new ProductClient.ProductSnapshot(1L, 29.99, 1, "IN_STOCK"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.place(2L, List.of(new OrderService.OrderLineRequest(1L, 2))));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test void cancelChecksCustomerAndAllowsOnlyEligibleStates() {
        CustomerOrder order = new CustomerOrder();
        order.setCustomerId(2L);
        when(repository.findById(5L)).thenReturn(Optional.of(order));

        ResponseStatusException wrongCustomer = assertThrows(ResponseStatusException.class, () -> service.cancel(5L, 3L));
        assertEquals(HttpStatus.FORBIDDEN, wrongCustomer.getStatusCode());
        CustomerOrder cancelled = service.cancel(5L, 2L);
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        verify(publisher).orderCancelled(order);
    }

    @Test void paymentEventMovesPendingOrderAndIgnoresDuplicateStateTransition() {
        CustomerOrder order = new CustomerOrder();
        order.setCustomerId(2L);
        when(repository.findById(5L)).thenReturn(Optional.of(order));
        service.recordPayment(new PaymentCompletedEvent(5L, true));
        assertEquals(OrderStatus.PAID, order.getStatus());
        service.recordPayment(new PaymentCompletedEvent(5L, true));
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(repository, times(1)).save(order);
    }
}
