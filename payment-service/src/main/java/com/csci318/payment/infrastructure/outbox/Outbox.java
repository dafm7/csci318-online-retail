package com.csci318.payment.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class Outbox {
    private final OutboxEventRepository repository;
    private final ObjectMapper mapper;

    public Outbox(OutboxEventRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public void enqueue(String bindingName, Object event) {
        try {
            repository.save(new OutboxEvent(UUID.randomUUID().toString(), bindingName,
                    mapper.writeValueAsString(event), Instant.now()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("could not serialize event for " + bindingName, e);
        }
    }
}
