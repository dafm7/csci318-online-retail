package com.csci318.agent.infrastructure.memory;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Conversation memory for the shopping assistant.
// Each chat session keeps its own sliding window of recent messages, held in memory (lost on restart).
@Configuration
public class ChatMemoryConfig {

    static final int MESSAGES_PER_SESSION = 30;

    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    // @AiService picks this up automatically and calls it with the @MemoryId of each request
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore store) {
        return sessionId -> MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(MESSAGES_PER_SESSION)
                .chatMemoryStore(store)
                .build();
    }
}
