package com.csci318.product.infrastructure.messaging;

import com.csci318.product.domain.event.OrderPlacedEvent;
import com.csci318.product.service.ProductService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class OrderPlacedEventHandler {

    // Bound to the "order-placed" topic in application.yml (reduceStock-in-0)
    @Bean
    public Consumer<OrderPlacedEvent> reduceStock(ProductService service) {
        return service::reduceStockForOrder;
    }
}
