package com.csci318.agent.infrastructure.client;

import com.csci318.agent.domain.OrderInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// REST client for order-service
@Component
public class OrderClient {

    private final RestClient api;

    public OrderClient(@Value("${services.order-url}") String baseUrl) {
        this.api = RestClient.create(baseUrl + "/api/orders");
    }

    public OrderInfo get(Long orderId) {
        return api.get().uri("/{id}", orderId).retrieve().body(OrderInfo.class);
    }

    public List<OrderInfo> forCustomer(Long customerId) {
        return List.of(api.get().uri("?customerId={c}", customerId).retrieve().body(OrderInfo[].class));
    }
}
