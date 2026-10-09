package com.csci318.agent.controller;

import com.csci318.agent.assistant.ShoppingAssistant;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ShoppingAssistant assistant;

    public ChatController(ShoppingAssistant assistant) { this.assistant = assistant; }

    // POST {"message": "do you sell keyboards?"}
    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        return Map.of("reply", assistant.chat(body.get("message")));
    }
}
