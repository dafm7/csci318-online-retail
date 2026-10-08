package com.csci318.product.event;

import com.csci318.product.repository.ProductRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class InventoryEventHandler {

    // Bound to the "order-placed" topic in application.yml (reduceStock-in-0)
    @Bean
    public Consumer<OrderPlacedEvent> reduceStock(ProductRepository repo) {
        return event -> repo.findById(event.productId()).ifPresent(p -> {
            p.setStock(p.getStock() - event.quantity());
            repo.save(p);
            System.out.println("[product-service] stock reduced for product " + p.getId());
        });
    }
}
