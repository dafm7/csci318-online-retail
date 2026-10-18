package com.csci318.chatclient.infrastructure;

import java.net.URI;
import java.net.http.HttpRequest;

// HTTP client for ai-agent-service's /api/chat endpoint
public class AgentApi extends JsonApi {

    public record ChatRequest(String sessionId, Long customerId, String message) {}
    public record ChatResponse(String sessionId, String reply) {}

    private final String chatUrl;

    public AgentApi(String baseUrl) { this.chatUrl = baseUrl + "/api/chat"; }

    // POST {"sessionId","customerId","message"} -> {"sessionId","reply"}
    public ChatResponse chat(ChatRequest request) throws ApiException {
        return post(chatUrl, request, ChatResponse.class);
    }

    // DELETE /api/chat/{sessionId}: the server forgets the conversation
    public void endSession(String sessionId) throws ApiException {
        send(HttpRequest.newBuilder(URI.create(chatUrl + "/" + sessionId)).DELETE().build());
    }
}
