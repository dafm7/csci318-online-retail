package com.csci318.chatclient;

import com.csci318.chatclient.infrastructure.AgentApi;
import com.csci318.chatclient.infrastructure.CustomerApi;
import com.csci318.chatclient.presentation.ChatWindow;
import com.csci318.chatclient.service.ChatSession;

import javax.swing.*;

// Desktop chat window for the shop assistant (ai-agent-service).
// A plain Java Swing app, not a Spring Boot service: run this main method from IntelliJ,
// or `mvn -pl chat-client spring-boot:run`. Override the service URLs with -Dagent.url=... and -Dcustomer.url=...
public class ChatClientApplication {

    public static void main(String[] args) {
        String agentUrl = System.getProperty("agent.url", "http://localhost:8085");
        String customerUrl = System.getProperty("customer.url", "http://localhost:8082");
        ChatSession session = new ChatSession(new AgentApi(agentUrl), new CustomerApi(customerUrl));

        UIManager.put("swing.boldMetal", Boolean.FALSE);   // plain default font, so **bold** in replies stands out
        SwingUtilities.invokeLater(() -> new ChatWindow(session).setVisible(true));
    }
}
