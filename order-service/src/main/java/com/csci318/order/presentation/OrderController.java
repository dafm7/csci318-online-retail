package com.csci318.order.presentation;

import com.csci318.order.domain.CustomerOrder;
import com.csci318.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    // All orders, or ?customerId=1 for one customer's orders
    @GetMapping
    public List<CustomerOrder> all(@RequestParam(required = false) Long customerId) { return service.find(customerId); }

    @GetMapping("/{id}")
    public CustomerOrder one(@PathVariable Long id) { return service.get(id); }

    // POST {"customerId","productId","quantity","amount"}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerOrder place(@RequestBody CustomerOrder order) { return service.place(order); }
}
