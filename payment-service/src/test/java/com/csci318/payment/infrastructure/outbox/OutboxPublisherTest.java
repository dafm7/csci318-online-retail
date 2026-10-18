package com.csci318.payment.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {
    @Test void recordsSuccessfulPublishAndLeavesRejectedMessageForRetry() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        StreamBridge bridge = mock(StreamBridge.class);
        OutboxEvent event = new OutboxEvent("id", "paymentCompleted-out-0", "{\"orderId\":7}", Instant.now());
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(bridge.send(eq("paymentCompleted-out-0"), any())).thenReturn(false, true);
        OutboxPublisher publisher = new OutboxPublisher(repository, new ObjectMapper(), bridge);

        publisher.publishPending();
        assertNull(event.getPublishedAt());
        assertEquals(1, event.getAttempts());
        publisher.publishPending();
        assertNotNull(event.getPublishedAt());
        assertEquals(2, event.getAttempts());
    }
}
