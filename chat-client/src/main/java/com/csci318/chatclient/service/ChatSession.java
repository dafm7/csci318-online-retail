package com.csci318.chatclient.service;

import com.csci318.chatclient.infrastructure.AgentApi;
import com.csci318.chatclient.infrastructure.AgentApi.ChatRequest;
import com.csci318.chatclient.infrastructure.CustomerApi;
import com.csci318.chatclient.infrastructure.CustomerApi.Customer;
import com.csci318.chatclient.infrastructure.JsonApi.ApiException;

import java.util.Optional;

// The client's state: the current conversation (sessionId from the server) and the signed-in customer.
// Methods block on the network, so the window calls them off the Swing event thread.
public class ChatSession {

    private final AgentApi agent;
    private final CustomerApi customers;
    private String sessionId;      // null until the server starts a conversation
    private Customer customer;     // null when signed out

    public ChatSession(AgentApi agent, CustomerApi customers) {
        this.agent = agent;
        this.customers = customers;
    }

    public synchronized String send(String message) {
        try {
            var res = agent.chat(new ChatRequest(sessionId, customer == null ? null : customer.id(), message));
            sessionId = res.sessionId();
            return res.reply();
        } catch (ApiException e) {
            return "Sorry, I couldn't reach the shop assistant. Is ai-agent-service running?";
        }
    }

    // Returns an error message to show, or empty on success
    public synchronized Optional<String> signIn(String email, String password) {
        try {
            customer = customers.login(email, password);
            return Optional.empty();
        } catch (ApiException e) {
            return Optional.of(e.status() == 401 ? "Wrong email or password."
                    : "Sign-in failed. Is user-service running?");
        }
    }

    // Signing out also starts a new conversation, so the next person can't scroll back through it
    public synchronized void signOut() {
        customer = null;
        newConversation();
    }

    public synchronized void newConversation() {
        if (sessionId != null) {
            try {
                agent.endSession(sessionId);
            } catch (ApiException ignored) {
                // the server forgets idle conversations on restart anyway
            }
        }
        sessionId = null;
    }

    public synchronized Optional<Customer> customer() { return Optional.ofNullable(customer); }
}
