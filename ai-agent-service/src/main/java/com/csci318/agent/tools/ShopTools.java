package com.csci318.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// The LLM can call these methods. Each one calls another microservice's REST API.
@Component
public class ShopTools {

    private final RestClient productApi = RestClient.create("http://localhost:8081/api/products");
    private final RestClient orderApi = RestClient.create("http://localhost:8083/api/orders");

    @Tool("Search products by name keyword. Returns JSON with id, name, category, price, stock.")
    public String searchProducts(@P("keyword to search for") String keyword) {
        return productApi.get().uri("?search={k}", keyword).retrieve().body(String.class);
    }

    @Tool("List all products in the shop")
    public String listProducts() {
        return productApi.get().retrieve().body(String.class);
    }

    @Tool("Get the status and details of an order by its order id")
    public String getOrder(@P("order id") Long orderId) {
        return orderApi.get().uri("/{id}", orderId).retrieve().body(String.class);
    }

    @Tool("List all orders for a customer id")
    public String ordersForCustomer(@P("customer id") Long customerId) {
        return orderApi.get().uri("?customerId={c}", customerId).retrieve().body(String.class);
    }
}
