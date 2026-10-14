package com.csci318.inventory.stream;

import com.csci318.inventory.event.LowStockAlert;
import com.csci318.inventory.event.StockChangedEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.function.Function;

@Configuration
public class InventoryStream {

    // Name of the state store holding the latest stock per product (queried by InventoryController)
    public static final String STOCK_STORE = "stock-levels";

    // in: stock-changed   out: low-stock-alerts
    @Bean
    public Function<KStream<String, StockChangedEvent>, KStream<String, LowStockAlert>> inventory(
            @Value("${inventory.low-stock-threshold}") int threshold) {
        return events -> {
            // Key every event by product id
            KStream<String, StockChangedEvent> byProduct =
                    events.selectKey((key, e) -> String.valueOf(e.productId()));

            // 1. Real-time stock count: keep only the latest stock per product in a queryable table
            byProduct.groupByKey(Grouped.with(Serdes.String(), new JsonSerde<>(StockChangedEvent.class)))
                    .reduce((previous, latest) -> latest,
                            Materialized.<String, StockChangedEvent, org.apache.kafka.streams.state.KeyValueStore<org.apache.kafka.common.utils.Bytes, byte[]>>as(STOCK_STORE)
                                    .withKeySerde(Serdes.String())
                                    .withValueSerde(new JsonSerde<>(StockChangedEvent.class)));

            // 2. Low-stock alerts: every change that leaves stock below the threshold raises an alert
            return byProduct
                    .filter((id, e) -> e.stock() < threshold)
                    .mapValues(e -> new LowStockAlert(e.productId(), e.name(), e.stock(), threshold))
                    .peek((id, a) -> System.out.println("[inventory-service] LOW STOCK ALERT: " + a.name()
                            + " (product " + a.productId() + ") has " + a.stock() + " left, threshold " + threshold));
        };
    }
}
