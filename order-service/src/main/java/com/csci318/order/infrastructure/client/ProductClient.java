package com.csci318.order.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class ProductClient {
    private final RestClient client;
    public ProductClient(@Value("${services.product-url:http://localhost:8081}") String url) {
        client = RestClient.builder().baseUrl(url).build();
    }
    public ProductSnapshot get(Long id) {
        try {
            ProductSnapshot product = client.get().uri("/api/products/{id}", id).retrieve().body(ProductSnapshot.class);
            if (product == null) throw new ProductNotFoundException(id);
            return product;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ProductNotFoundException(id);
        } catch (RestClientException ex) {
            throw new ProductServiceUnavailableException(ex);
        }
    }
    public record ProductSnapshot(Long id, double price, int stock, String availability) {}

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(Long id) { super("product not found: " + id); }
    }
    public static class ProductServiceUnavailableException extends RuntimeException {
        public ProductServiceUnavailableException(Throwable cause) { super("product service is unavailable", cause); }
    }
}
