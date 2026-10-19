package com.csci318.payment.infrastructure.analysis;

import com.csci318.payment.domain.event.PaymentAnalysisEvent;
import com.csci318.payment.infrastructure.stream.PaymentAnalysisStream;
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

// Runs the real PaymentAnalysisStream topology in memory (no Kafka broker) and reads the summary from its stores
class PaymentAnalysisMetricsTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private TopologyTestDriver driver;
    private TestInputTopic<String, PaymentAnalysisEvent> input;
    private PaymentAnalysisMetrics metrics;

    @BeforeEach
    void setUp() throws Exception {
        JsonSerde<PaymentAnalysisEvent> serde = new JsonSerde<>(PaymentAnalysisEvent.class);
        StreamsBuilder builder = new StreamsBuilder();
        PaymentAnalysisStream.build(builder.stream("payment-analysis", Consumed.with(Serdes.String(), serde)));
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "payment-analytics-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "unused:9092");
        props.put(StreamsConfig.STATE_DIR_CONFIG, Files.createTempDirectory("payment-analytics-test").toString());
        driver = new TopologyTestDriver(builder.build(), props);
        input = driver.createInputTopic("payment-analysis", new StringSerializer(), serde.serializer());
        metrics = new PaymentAnalysisMetrics(new PaymentAnalysisStores() {
            public ReadOnlyKeyValueStore<String, Long> paymentsPerStatus() { return driver.getKeyValueStore(PaymentAnalysisStream.PAYMENTS_PER_STATUS_STORE); }
            public ReadOnlyKeyValueStore<String, Double> paidAmount() { return driver.getKeyValueStore(PaymentAnalysisStream.PAID_AMOUNT_STORE); }
            public ReadOnlyWindowStore<String, Long> updatesPerWindow() { return driver.getWindowStore(PaymentAnalysisStream.UPDATES_PER_WINDOW_STORE); }
        });
    }

    @AfterEach
    void tearDown() { driver.close(); }

    private void send(String id, long orderId, String status, double amount, Instant at) {
        input.pipeInput(null, new PaymentAnalysisEvent(id, orderId, status, amount, at), at);
    }

    @Test
    void successRateExcludesPendingPaymentsAndUsesLatestState() {
        send("1", 1, "PENDING", 25.00, T0);
        send("2", 1, "SUCCESS", 25.00, T0.plusSeconds(1));
        send("3", 2, "FAILED", 1200.00, T0.plusSeconds(2));
        send("4", 3, "PENDING", 10.00, T0.plusSeconds(3));

        Map<String, Object> result = metrics.snapshot(T0.plusSeconds(5));

        assertEquals(3L, result.get("totalPayments"));
        assertEquals(0.5, result.get("successRate"));
        assertEquals(25.00, (double) result.get("totalAmount"), 0.001);
        assertEquals(4L, result.get("paymentUpdatesLastMinute"));
        assertEquals(Map.of("PENDING", 1L, "SUCCESS", 1L, "FAILED", 1L, "REFUNDED", 0L), result.get("statusCounts"));
    }

    @Test
    void refundKeepsTheAmountAndOldUpdatesLeaveTheWindow() {
        send("1", 1, "PENDING", 40.00, T0);
        send("2", 1, "SUCCESS", 40.00, T0.plusSeconds(1));
        send("3", 1, "REFUNDED", 40.00, T0.plusSeconds(2));

        Map<String, Object> later = metrics.snapshot(T0.plusSeconds(600));
        assertEquals(40.00, (double) later.get("totalAmount"), 0.001);
        assertEquals(Map.of("PENDING", 0L, "SUCCESS", 0L, "FAILED", 0L, "REFUNDED", 1L), later.get("statusCounts"));
        assertEquals(0L, later.get("paymentUpdatesLastMinute"));
    }
}
