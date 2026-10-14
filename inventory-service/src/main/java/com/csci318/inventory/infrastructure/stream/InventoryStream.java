package com.csci318.inventory.infrastructure.stream;

import com.csci318.inventory.domain.LowStockPolicy;
import com.csci318.inventory.domain.event.LowStockAlert;
import com.csci318.inventory.domain.event.StockChangedEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.function.Function;

// Kafka Streams topology. in: stock-changed   out: low-stock-alerts
@Configuration
public class InventoryStream {

    // Name of the state store holding the latest stock per product (read by StockLevelStore)
    public static final String STOCK_STORE = "stock-levels";

    @Bean
    public Function<KStream<String, StockChangedEvent>, KStream<String, LowStockAlert>> inventory(LowStockPolicy policy) {
        return events -> {
            // Key every event by product id
            KStream<String, StockChangedEvent> byProduct =
                    events.selectKey((key, e) -> String.valueOf(e.productId()));

            // 1. Real-time stock count: keep only the latest stock per product in a queryable table
            JsonSerde<StockChangedEvent> serde = new JsonSerde<>(StockChangedEvent.class);
            byProduct.groupByKey(Grouped.with(Serdes.String(), serde))
                    .reduce((previous, latest) -> latest,
                            Materialized.<String, StockChangedEvent, KeyValueStore<Bytes, byte[]>>as(STOCK_STORE)
                                    .withKeySerde(Serdes.String())
                                    .withValueSerde(serde));

            // 2. Low-stock alerts: every change that leaves stock below the threshold raises an alert
            return byProduct
                    .filter((id, e) -> policy.isLow(e))
                    .mapValues(policy::alertFor)
                    .peek((id, a) -> System.out.println("[inventory-service] LOW STOCK ALERT: " + a.name()
                            + " (product " + a.productId() + ") has " + a.stock() + " left, threshold " + a.threshold()));
        };
    }
}
