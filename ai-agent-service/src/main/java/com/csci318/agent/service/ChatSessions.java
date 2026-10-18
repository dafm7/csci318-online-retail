package com.csci318.agent.service;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Keeps track of which customer (if any) is signed in to each chat session.
// The tools look the customer up here instead of trusting an id the LLM passes them,
// so the assistant can only ever show a customer their own orders and profile.
@Service
public class ChatSessions {

    private final Map<String, Long> customerBySession = new ConcurrentHashMap<>();
    private final ChatMemoryStore memory;

    public ChatSessions(ChatMemoryStore memory) { this.memory = memory; }

    public String newSessionId() { return UUID.randomUUID().toString(); }

    // Called on every message, so signing in or out mid-conversation takes effect straight away
    public void signIn(String sessionId, Long customerId) {
        if (customerId == null) customerBySession.remove(sessionId);
        else customerBySession.put(sessionId, customerId);
    }

    public Optional<Long> customerFor(String sessionId) {
        return Optional.ofNullable(customerBySession.get(sessionId));
    }

    // Forget the conversation and the signed-in customer
    public void end(String sessionId) {
        customerBySession.remove(sessionId);
        memory.deleteMessages(sessionId);
    }
}
