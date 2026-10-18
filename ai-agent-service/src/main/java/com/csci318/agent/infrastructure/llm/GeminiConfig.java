package com.csci318.agent.infrastructure.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The Gemini chat model used by the @AiService.
// Built here rather than by the LangChain4j Spring starter because the starter can't turn on
// returnThinking/sendThinking. Gemini 3 models reject a tool call unless the "thought signature"
// they attached to it is sent back with the tool result, and these two settings make LangChain4j
// keep the signature and send it back.
@Configuration
public class GeminiConfig {

    @Bean
    public ChatModel chatModel(@Value("${gemini.api-key}") String apiKey,
                               @Value("${gemini.model-name}") String modelName,
                               @Value("${gemini.base-url:}") String baseUrl) {
        var builder = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .returnThinking(true)
                .sendThinking(true);
        if (!baseUrl.isBlank()) builder.baseUrl(baseUrl);
        return builder.build();
    }
}
