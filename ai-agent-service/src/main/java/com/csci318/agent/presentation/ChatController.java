package com.csci318.agent.presentation;

import com.csci318.agent.service.ChatSessions;
import com.csci318.agent.service.ShoppingAssistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ShoppingAssistant assistant;
    private final ChatSessions sessions;

    public ChatController(ShoppingAssistant assistant, ChatSessions sessions) {
        this.assistant = assistant;
        this.sessions = sessions;
    }

    // sessionId and customerId are optional: leave sessionId out to start a new conversation,
    // and send customerId (from login) so the assistant can answer questions about "my orders".
    public record ChatRequest(String sessionId, Long customerId, String message) {}
    public record ChatResponse(String sessionId, String reply) {}

    // POST {"message": "do you sell keyboards?"}  ->  {"sessionId": "...", "reply": "..."}
    // POST {"sessionId": "...", "customerId": 1, "message": "where is my last order?"}
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? sessions.newSessionId() : request.sessionId();
        if (request.message() == null || request.message().isBlank()) {
            return new ChatResponse(sessionId, "Please type a question.");
        }
        sessions.signIn(sessionId, request.customerId());
        try {
            return new ChatResponse(sessionId, assistant.chat(sessionId, request.message()));
        } catch (RuntimeException e) {
            // Usually the LLM is unreachable (no API key, rate limit) or another service is down
            log.warn("chat failed for session {}", sessionId, e);
            return new ChatResponse(sessionId,
                    "Sorry, I can't answer right now. Please try again in a moment.");
        }
    }

    // DELETE /api/chat/{sessionId}: forget the conversation (the chat window's "new chat" button)
    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endSession(@PathVariable String sessionId) { sessions.end(sessionId); }
}
