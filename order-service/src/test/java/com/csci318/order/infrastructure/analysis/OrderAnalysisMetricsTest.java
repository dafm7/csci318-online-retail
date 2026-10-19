package com.csci318.order.infrastructure.analysis;

import com.csci318.order.domain.event.OrderAnalysisEvent;
import com.csci318.order.infrastructure.stream.OrderAnalysisStream;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Runs the real OrderAnalysisStream topology in memory (no Kafka broker) and reads the summary from its stores
class OrderAnalysisMetricsTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private TopologyTestDriver driver;
    private TestInputTopic<String, OrderAnalysisEvent> input;
    private OrderAnalysisMetrics metrics;

    @BeforeEach
    void setUp() throws Exception {
        JsonSerde<OrderAnalysisEvent> serde = new JsonSerde<>(OrderAnalysisEvent.class);
        StreamsBuilder builder = new StreamsBuilder();
        OrderAnalysisStream.build(builder.stream("order-analysis", Consumed.with(Serdes.String(), serde)));
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-analytics-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "unused:9092");
        props.put(StreamsConfig.STATE_DIR_CONFIG, Files.createTempDirectory("order-analytics-test").toString());
        driver = new TopologyTestDriver(builder.build(), props);
        input = driver.createInputTopic("order-analysis", new StringSerializer(), serde.serializer());
        metrics = new OrderAnalysisMetrics(new OrderAnalysisStores() {
            public ReadOnlyKeyValueStore<String, Long> ordersPerStatus() { return driver.getKeyValueStore(OrderAnalysisStream.ORDERS_PER_STATUS_STORE); }
            public ReadOnlyWindowStore<String, Long> eventsPerWindow() { return driver.getWindowStore(OrderAnalysisStream.EVENTS_PER_WINDOW_STORE); }
        });
    }

    @AfterEach
    void tearDown() { driver.close(); }

    private void send(String id, long orderId, String type, String previous, String status, Instant at) {
        input.pipeInput(null, new OrderAnalysisEvent(id, orderId, type, previous, status, at), at);
    }

    @Test
    void summaryUsesLatestStatusOfEachOrder() {
        send("1", 7, "ORDER_CREATED", null, "PENDING", T0);
        send("2", 7, "ORDER_STATUS_CHANGED", "PENDING", "PAID", T0.plusSeconds(1));
        send("3", 7, "ORDER_CANCELLED", "PAID", "CANCELLED", T0.plusSeconds(2));
        send("4", 8, "ORDER_CREATED", null, "PENDING", T0.plusSeconds(3));

        Map<String, Object> result = metrics.snapshot(T0.plusSeconds(5));

        assertEquals(2L, result.get("totalOrders"));
        assertEquals(Map.of("CANCELLED", 1L, "PENDING", 1L), result.get("statusCounts"));
        assertEquals(0.5, result.get("cancellationRate"));
        assertEquals(2L, result.get("ordersLastMinute"));
        assertEquals(1L, result.get("cancelledLastMinute"));
        assertEquals(2L, result.get("statusChangesLastMinute"));
    }

    @Test
    void lastMinuteUsesTheHoppingWindowCoveringTheLast60Seconds() {
        send("1", 1, "ORDER_CREATED", null, "PENDING", T0);
        send("2", 2, "ORDER_CREATED", null, "PENDING", T0.plusSeconds(55));

        // At 10:01:10 the window [10:00:20, 10:01:20) covers the last 60 s: only the 10:00:55 order is in it
        assertEquals(1L, metrics.snapshot(T0.plusSeconds(70)).get("ordersLastMinute"));
        // Ten minutes later nothing is recent, but the totals are unchanged
        Map<String, Object> later = metrics.snapshot(T0.plusSeconds(600));
        assertEquals(0L, later.get("ordersLastMinute"));
        assertEquals(2L, later.get("totalOrders"));
    }

    @Test
    void redeliveredEventDoesNotChangeTheStatusCounts() {
        send("1", 7, "ORDER_CREATED", null, "PENDING", T0);
        send("1", 7, "ORDER_CREATED", null, "PENDING", T0.plusSeconds(1));   // Kafka redelivery

        Map<String, Object> result = metrics.snapshot(T0.plusSeconds(5));
        assertEquals(1L, result.get("totalOrders"));
        assertEquals(Map.of("PENDING", 1L), result.get("statusCounts"));
    }
}
