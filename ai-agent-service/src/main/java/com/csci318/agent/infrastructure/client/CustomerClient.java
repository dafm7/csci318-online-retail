package com.csci318.agent.infrastructure.client;

import com.csci318.agent.domain.CustomerInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// REST client for user-service (the shop's customers are its users)
@Component
public class CustomerClient {

    private final RestClient api;

    public CustomerClient(@Value("${services.customer-url}") String baseUrl) {
        this.api = RestClient.create(baseUrl + "/api/users");
    }

    public CustomerInfo profile(Long customerId) {
        return api.get().uri("/{id}", customerId).retrieve().body(CustomerInfo.class);
    }
}
