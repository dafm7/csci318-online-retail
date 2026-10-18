package com.csci318.product.infrastructure.stream;

import com.csci318.product.domain.ProductSales;
import com.csci318.product.domain.event.OrderPlacedEvent;
import com.csci318.product.domain.event.OrderLineEvent;
import com.csci318.product.domain.event.PaymentCompletedEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.function.BiConsumer;

// Kafka Streams topology for real-time sales analysis. in-0: order-placed   in-1: payment-completed
@Configuration
public class SalesStream {

    public static final String SALES_STORE = "product-sales";          // all-time totals per product
    public static final String UNITS_PER_MINUTE_STORE = "units-per-minute"; // 1-minute windows per product

    @Bean
    public BiConsumer<KStream<String, OrderPlacedEvent>, KStream<String, PaymentCompletedEvent>> sales() {
        return (orders, payments) -> {
            JsonSerde<OrderPlacedEvent> orderSerde = new JsonSerde<>(OrderPlacedEvent.class);
            JsonSerde<PaymentCompletedEvent> paymentSerde = new JsonSerde<>(PaymentCompletedEvent.class);

            // 1. JOIN: match each order with its payment result (same orderId, within 5 minutes); keep only paid ones
            KStream<String, OrderPlacedEvent> paidOrders = orders
                    .selectKey((k, o) -> String.valueOf(o.orderId()))
                    .join(payments.selectKey((k, p) -> String.valueOf(p.orderId())),
                            (order, payment) -> payment.success() ? order : null,
                            JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5)),
                            StreamJoined.with(Serdes.String(), orderSerde, paymentSerde))
                    .filter((orderId, order) -> order != null);

            // Expand each paid order into product lines before aggregating sales.
            JsonSerde<OrderLineEvent> lineSerde = new JsonSerde<>(OrderLineEvent.class);
            KGroupedStream<String, OrderLineEvent> paidByProduct = paidOrders
                    .flatMapValues(OrderPlacedEvent::lines)
                    .selectKey((orderId, line) -> String.valueOf(line.productId()))
                    .groupByKey(Grouped.with(Serdes.String(), lineSerde));

            // 2. AGGREGATE: running units sold, revenue and order count per product
            paidByProduct.aggregate(ProductSales::none,
                    (productId, line, totals) -> totals.add(line),
                    Materialized.<String, ProductSales, KeyValueStore<Bytes, byte[]>>as(SALES_STORE)
                            .withKeySerde(Serdes.String())
                            .withValueSerde(new JsonSerde<>(ProductSales.class)))
                    .toStream()
                    .foreach((productId, s) -> System.out.println("[product-service] SALES product " + productId
                            + ": " + s.unitsSold() + " units, $" + s.revenue() + " from " + s.orders() + " orders"));

            // 3. WINDOW: units sold per product in each 1-minute tumbling window
            paidByProduct.windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
                    .aggregate(() -> 0L,
                            (productId, line, units) -> units + line.quantity(),
                            Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(UNITS_PER_MINUTE_STORE)
                                    .withKeySerde(Serdes.String())
                                    .withValueSerde(Serdes.Long()));
        };
    }
}
