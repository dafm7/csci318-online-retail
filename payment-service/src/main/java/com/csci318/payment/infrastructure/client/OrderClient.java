package com.csci318.payment.infrastructure.client;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class OrderClient {
    private final RestClient client;
    public OrderClient(@Value("${services.order-url:http://localhost:8083}") String url) {
        client = RestClient.builder().baseUrl(url).build();
    }
    public OrderSnapshot get(Long id) {
        try {
            OrderSnapshot order = client.get().uri("/api/orders/{id}", id).retrieve().body(OrderSnapshot.class);
            if (order == null) throw new OrderNotFoundException(id);
            return order;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new OrderNotFoundException(id);
        } catch (RestClientException ex) {
            throw new OrderServiceUnavailableException(ex);
        }
    }
    public record OrderSnapshot(Long id, BigDecimal amount, String status) {}

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(Long id) { super("order not found: " + id); }
    }
    public static class OrderServiceUnavailableException extends RuntimeException {
        public OrderServiceUnavailableException(Throwable cause) { super("order service is unavailable", cause); }
    }
}
