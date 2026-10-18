package com.csci318.product.infrastructure.messaging;

import com.csci318.product.domain.event.OrderCancelledEvent;
import com.csci318.product.service.ProductService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Consumer;

@Configuration
public class OrderCancelledEventHandler {
    @Bean
    public Consumer<OrderCancelledEvent> restoreCancelledOrderStock(ProductService service) {
        return service::restoreStockForCancelledOrder;
    }
}
