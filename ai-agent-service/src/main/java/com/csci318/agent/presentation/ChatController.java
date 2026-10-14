package com.csci318.agent.presentation;

import com.csci318.agent.service.ShoppingAssistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ShoppingAssistant assistant;

    public ChatController(ShoppingAssistant assistant) { this.assistant = assistant; }

    public record ChatRequest(String message) {}
    public record ChatResponse(String reply) {}

    // POST {"message": "do you sell keyboards?"}  ->  {"reply": "..."}
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return new ChatResponse(assistant.chat(request.message()));
    }
}
