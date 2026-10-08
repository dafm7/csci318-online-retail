package com.csci318.agent.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

// LangChain4j generates the implementation; it can use any @Tool bean (ShopTools).
@AiService
public interface ShoppingAssistant {

    @SystemMessage("""
            You are a helpful assistant for an online retail shop.
            Help customers find products and check their orders.
            Always use the tools to get real data. Never make up products, prices or orders.
            """)
    String chat(String userMessage);
}
