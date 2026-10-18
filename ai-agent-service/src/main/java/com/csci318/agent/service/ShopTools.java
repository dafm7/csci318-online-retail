package com.csci318.agent.service;

import com.csci318.agent.domain.OrderInfo;
import com.csci318.agent.domain.ProductInfo;
import com.csci318.agent.infrastructure.client.CustomerClient;
import com.csci318.agent.infrastructure.client.OrderClient;
import com.csci318.agent.infrastructure.client.ProductClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

// The LLM can call these methods. Each one uses a client to call another microservice's REST API.
// Customer-specific tools take the chat session id (@ToolMemoryId, filled in by LangChain4j, never by the LLM)
// and only ever read the data of the customer signed in to that session.
@Component
public class ShopTools {

    static final String NOT_SIGNED_IN = "Nobody is signed in to this chat, so customer data is not available.";

    private final ProductClient products;
    private final OrderClient orders;
    private final CustomerClient customers;
    private final ChatSessions sessions;

    public ShopTools(ProductClient products, OrderClient orders, CustomerClient customers, ChatSessions sessions) {
        this.products = products;
        this.orders = orders;
        this.customers = customers;
        this.sessions = sessions;
    }

    // ---- Product catalogue (anyone) ----

    @Tool("Search products by name keyword. Returns id, name, category, price, stock.")
    public List<ProductInfo> searchProducts(@P("keyword to search for") String keyword) { return products.search(keyword); }

    @Tool("List the products in one category, for example Electronics or Sport")
    public List<ProductInfo> productsInCategory(@P("category name") String category) { return products.inCategory(category); }

    @Tool("Get one product by its product id")
    public Object getProduct(@P("product id") Long productId) {
        try {
            return products.get(productId);
        } catch (HttpClientErrorException.NotFound e) {
            return "There is no product with id " + productId + ".";
        }
    }

    @Tool("List all products in the shop")
    public List<ProductInfo> listProducts() { return products.all(); }

    // ---- The signed-in customer's own data ----

    @Tool("List all orders of the customer signed in to this chat")
    public Object myOrders(@ToolMemoryId String sessionId) {
        return signedIn(sessionId).<Object>map(orders::forCustomer).orElse(NOT_SIGNED_IN);
    }

    @Tool("Get the status and details of one of the signed-in customer's orders by its order id")
    public Object myOrder(@ToolMemoryId String sessionId, @P("order id") Long orderId) {
        Optional<Long> customerId = signedIn(sessionId);
        if (customerId.isEmpty()) return NOT_SIGNED_IN;
        try {
            OrderInfo order = orders.get(orderId);
            // Someone else's order is reported exactly like a missing one, so order ids can't be probed
            if (order != null && customerId.get().equals(order.customerId())) return order;
        } catch (HttpClientErrorException.NotFound e) {
            // fall through
        }
        return "The signed-in customer has no order with id " + orderId + ".";
    }

    @Tool("Get the signed-in customer's profile: name, email, address and a summary of their orders")
    public Object myProfile(@ToolMemoryId String sessionId) {
        return signedIn(sessionId).<Object>map(customers::profile).orElse(NOT_SIGNED_IN);
    }

    private Optional<Long> signedIn(String sessionId) { return sessions.customerFor(sessionId); }
}
