package com.csci318.payment.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository repository;
    private final ObjectMapper mapper;
    private final StreamBridge streamBridge;

    public OutboxPublisher(OutboxEventRepository repository, ObjectMapper mapper, StreamBridge streamBridge) {
        this.repository = repository;
        this.mapper = mapper;
        this.streamBridge = streamBridge;
    }

    @Scheduled(fixedDelayString = "${outbox.publish-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            event.recordAttempt();
            try {
                boolean sent = streamBridge.send(event.getBindingName(), mapper.readTree(event.getPayload()));
                if (sent) event.markPublished(Instant.now());
                else log.warn("Kafka did not accept outbox event {} on {}", event.getId(), event.getBindingName());
            } catch (Exception e) {
                log.warn("Could not publish outbox event {} on {}; it will be retried", event.getId(), event.getBindingName(), e);
            }
        }
    }
}
