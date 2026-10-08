package com.csci318.order.controller;

import com.csci318.order.event.OrderPlacedEvent;
import com.csci318.order.model.CustomerOrder;
import com.csci318.order.repository.OrderRepository;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    private final OrderRepository repo;
    private final StreamBridge streamBridge;

    public OrderController(OrderRepository repo, StreamBridge streamBridge) {
        this.repo = repo;
        this.streamBridge = streamBridge;
    }

    @GetMapping
    public List<CustomerOrder> all(@RequestParam(required = false) Long customerId) {
        return (customerId == null) ? repo.findAll() : repo.findByCustomerId(customerId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerOrder> one(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Save the order, then publish an OrderPlaced event to Kafka
    @PostMapping
    public CustomerOrder place(@RequestBody CustomerOrder order) {
        CustomerOrder saved = repo.save(order);
        streamBridge.send("orderPlaced-out-0", new OrderPlacedEvent(
                saved.getId(), saved.getCustomerId(), saved.getProductId(),
                saved.getQuantity(), saved.getAmount()));
        return saved;
    }
}
