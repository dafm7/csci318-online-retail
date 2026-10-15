package com.csci318.customer.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

// REST client for order-service (used to show a customer's order history on their profile)
@Component
public class OrderClient {

    public record OrderData(Long id, Long productId, int quantity, double amount, String status) {}

    private final RestClient api;

    public OrderClient(@Value("${services.order-url}") String baseUrl) {
        this.api = RestClient.create(baseUrl + "/api/orders");
    }

    // Empty if order-service is not reachable, so the profile still works without it
    public Optional<List<OrderData>> ordersFor(Long customerId) {
        try {
            return Optional.of(List.of(api.get().uri("?customerId={c}", customerId).retrieve().body(OrderData[].class)));
        } catch (RestClientException e) {
            System.out.println("[customer-service] order-service unavailable: " + e.getMessage());
            return Optional.empty();
        }
    }
}
