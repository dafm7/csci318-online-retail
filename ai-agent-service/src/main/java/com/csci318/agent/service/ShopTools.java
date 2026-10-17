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

    @Tool("""
            Find products. Every filter is optional and they can be combined, e.g. "electronics under $100, cheapest first".
            Returns id, name, category, price, stock, availability.""")
    public List<ProductInfo> findProducts(
            @P(value = "part of the product name, e.g. keyboard", required = false) String keyword,
            @P(value = "one of CLOTHES, ELECTRONICS, SPORTS, HOME", required = false) String category,
            @P(value = "minimum price in dollars", required = false) Double minPrice,
            @P(value = "maximum price in dollars", required = false) Double maxPrice,
            @P(value = "true to only show products that are in stock", required = false) Boolean inStockOnly,
            @P(value = "one of price, price_desc, name, name_desc, stock, stock_desc", required = false) String sort) {
        return products.find(keyword, category, minPrice, maxPrice, inStockOnly, sort);
    }

    @Tool("List all products in the shop")
    public List<ProductInfo> listProducts() { return products.all(); }

    @Tool("Get the status and details of an order by its order id")
    public OrderInfo getOrder(@P("order id") Long orderId) { return orders.get(orderId); }

    @Tool("List all orders for a customer id")
    public List<OrderInfo> ordersForCustomer(@P("customer id") Long customerId) { return orders.forCustomer(customerId); }
}
