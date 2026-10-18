package com.csci318.chatclient.infrastructure;

// HTTP client for customer-service's login endpoint
public class CustomerApi extends JsonApi {

    public record LoginRequest(String email, String password) {}
    public record Customer(Long id, String name) {}

    private final String loginUrl;

    public CustomerApi(String baseUrl) { this.loginUrl = baseUrl + "/api/customers/login"; }

    // Throws ApiException with status 401 when the email or password is wrong
    public Customer login(String email, String password) throws ApiException {
        return post(loginUrl, new LoginRequest(email, password), Customer.class);
    }
}
