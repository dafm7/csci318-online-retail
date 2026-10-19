package com.csci318.payment.infrastructure.stream;

import com.csci318.payment.domain.event.PaymentAnalysisEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.function.Consumer;

// Kafka Streams topology for real-time payment analysis. in: payment-analysis
@Configuration
public class PaymentAnalysisStream {

    public static final String LATEST_PAYMENT_STORE = "payment-latest";            // latest payment state per order
    public static final String PAYMENTS_PER_STATUS_STORE = "payments-per-status";
    public static final String PAID_AMOUNT_STORE = "payments-paid-amount";         // key TOTAL
    public static final String UPDATES_PER_WINDOW_STORE = "payment-updates-per-window";
    public static final String TOTAL = "TOTAL";

    // Hopping window: 60 seconds long, a new window starts every 10 seconds
    public static final Duration WINDOW_SIZE = Duration.ofSeconds(60);
    public static final Duration WINDOW_ADVANCE = Duration.ofSeconds(10);

    @Bean
    public Consumer<KStream<String, PaymentAnalysisEvent>> paymentAnalytics() {
        return PaymentAnalysisStream::build;
    }

    public static void build(KStream<String, PaymentAnalysisEvent> events) {
        JsonSerde<PaymentAnalysisEvent> serde = new JsonSerde<>(PaymentAnalysisEvent.class);
        KStream<String, PaymentAnalysisEvent> valid = events
                .filter((k, e) -> e != null && e.orderId() != null && e.status() != null);

        // 1. TABLE: the latest payment state of every order
        KTable<String, PaymentAnalysisEvent> latest = valid
                .selectKey((k, e) -> String.valueOf(e.orderId()))
                .groupByKey(Grouped.with(Serdes.String(), serde))
                .reduce((previous, newer) -> newer,
                        Materialized.<String, PaymentAnalysisEvent, KeyValueStore<Bytes, byte[]>>as(LATEST_PAYMENT_STORE)
                                .withKeySerde(Serdes.String()).withValueSerde(serde));

        // 2. AGGREGATION: payments per status (a payment moves from its old status to its new one)
        latest.groupBy((orderId, e) -> KeyValue.pair(e.status(), orderId), Grouped.with(Serdes.String(), Serdes.String()))
                .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as(PAYMENTS_PER_STATUS_STORE)
                        .withKeySerde(Serdes.String()).withValueSerde(Serdes.Long()));

        // 3. AGGREGATION: total amount of payments whose latest state is SUCCESS or REFUNDED
        latest.groupBy((orderId, e) -> KeyValue.pair(TOTAL, e), Grouped.with(Serdes.String(), serde))
                .aggregate(() -> 0.0,
                        (k, e, sum) -> cents(sum + paidAmount(e)),
                        (k, e, sum) -> cents(sum - paidAmount(e)),
                        Materialized.<String, Double, KeyValueStore<Bytes, byte[]>>as(PAID_AMOUNT_STORE)
                                .withKeySerde(Serdes.String()).withValueSerde(Serdes.Double()));

        // 4. HOPPING WINDOW: payment updates in each 60-second window
        valid.groupBy((k, e) -> TOTAL, Grouped.with(Serdes.String(), serde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(WINDOW_SIZE).advanceBy(WINDOW_ADVANCE))
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(UPDATES_PER_WINDOW_STORE)
                        .withKeySerde(Serdes.String()).withValueSerde(Serdes.Long()));
    }

    private static double paidAmount(PaymentAnalysisEvent e) {
        return "SUCCESS".equals(e.status()) || "REFUNDED".equals(e.status()) ? e.amount() : 0;
    }

    private static double cents(double v) { return Math.round(v * 100) / 100.0; }
}
