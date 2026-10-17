package com.csci318.product.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

// REST client for user-service (used to check whether a caller is an admin)
@Component
public class UserClient {

    public record UserData(Long id, String name, String role) {}

    private final RestClient api;

    public UserClient(@Value("${services.user-url}") String baseUrl) {
        this.api = RestClient.create(baseUrl + "/api/users");
    }

    // Empty if the user does not exist. Throws ResourceAccessException if user-service is down.
    public Optional<UserData> find(Long userId) {
        try {
            return Optional.ofNullable(api.get().uri("/{id}", userId).retrieve().body(UserData.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
