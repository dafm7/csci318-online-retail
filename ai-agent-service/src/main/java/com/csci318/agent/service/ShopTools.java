package com.csci318.agent.service;

import com.csci318.agent.domain.OrderInfo;
import com.csci318.agent.domain.ProductInfo;
import com.csci318.agent.infrastructure.client.OrderClient;
import com.csci318.agent.infrastructure.client.ProductClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

// The LLM can call these methods. Each one uses a client to call another microservice's REST API.
@Component
public class ShopTools {

    private final ProductClient products;
    private final OrderClient orders;

    public ShopTools(ProductClient products, OrderClient orders) {
        this.products = products;
        this.orders = orders;
    }

    @Tool("Search products by name keyword. Returns id, name, category, price, stock.")
    public List<ProductInfo> searchProducts(@P("keyword to search for") String keyword) { return products.search(keyword); }

    @Tool("List all products in the shop")
    public List<ProductInfo> listProducts() { return products.all(); }

    @Tool("Get the status and details of an order by its order id")
    public OrderInfo getOrder(@P("order id") Long orderId) { return orders.get(orderId); }

    @Tool("List all orders for a customer id")
    public List<OrderInfo> ordersForCustomer(@P("customer id") Long customerId) { return orders.forCustomer(customerId); }
}
