package com.csci318.agent.service;

import com.csci318.agent.domain.OrderInfo;
import com.csci318.agent.infrastructure.client.CustomerClient;
import com.csci318.agent.infrastructure.client.OrderClient;
import com.csci318.agent.infrastructure.client.ProductClient;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// The customer tools must only ever return data for the customer signed in to the chat session
class ShopToolsTest {

    private static final OrderInfo ALICES_ORDER = new OrderInfo(10L, 1L, 2L, 1, 29.99, "PAID", List.of());
    private static final OrderInfo BOBS_ORDER = new OrderInfo(20L, 2L, 3L, 1, 9.99, "PAID", List.of());

    private final OrderClient orders = mock(OrderClient.class);
    private final CustomerClient customers = mock(CustomerClient.class);
    private ChatSessions sessions;
    private ShopTools tools;

    @BeforeEach
    void setUp() {
        sessions = new ChatSessions(new InMemoryChatMemoryStore());
        tools = new ShopTools(mock(ProductClient.class), orders, customers, sessions);
        when(orders.get(10L)).thenReturn(ALICES_ORDER);
        when(orders.get(20L)).thenReturn(BOBS_ORDER);
    }

    @Test
    void customerToolsNeedSomeoneSignedIn() {
        assertThat(tools.myOrders("s1")).isEqualTo(ShopTools.NOT_SIGNED_IN);
        assertThat(tools.myOrder("s1", 10L)).isEqualTo(ShopTools.NOT_SIGNED_IN);
        assertThat(tools.myProfile("s1")).isEqualTo(ShopTools.NOT_SIGNED_IN);
        verifyNoInteractions(customers);
    }

    @Test
    void signedInCustomerSeesOwnOrders() {
        sessions.signIn("s1", 1L);
        when(orders.forCustomer(1L)).thenReturn(List.of(ALICES_ORDER));

        assertThat(tools.myOrder("s1", 10L)).isEqualTo(ALICES_ORDER);
        assertThat(tools.myOrders("s1")).isEqualTo(List.of(ALICES_ORDER));
    }

    @Test
    void anotherCustomersOrderLooksLikeAMissingOrder() {
        sessions.signIn("s1", 1L);

        assertThat(tools.myOrder("s1", 20L)).isEqualTo("The signed-in customer has no order with id 20.");
    }

    @Test
    void signingOutStopsAccess() {
        sessions.signIn("s1", 1L);
        sessions.signIn("s1", null);

        assertThat(tools.myOrder("s1", 10L)).isEqualTo(ShopTools.NOT_SIGNED_IN);
    }
}
