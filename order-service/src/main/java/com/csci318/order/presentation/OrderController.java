package com.csci318.order.presentation;

import com.csci318.order.domain.CustomerOrder;
import com.csci318.order.domain.OrderStatus;
import com.csci318.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerOrder place(@RequestBody PlaceOrderRequest req) {
        if (req == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        List<OrderService.OrderLineRequest> lines = req.items() == null
                ? (req.productId() == null ? List.of() : List.of(new OrderService.OrderLineRequest(req.productId(), req.quantity())))
                : req.items().stream().map(i -> i == null ? null : new OrderService.OrderLineRequest(i.productId(), i.quantity())).toList();
        return service.place(req.customerId(), lines);
    }

    // Prototype identity: customerId stands in for the authenticated user.
    @PostMapping("/{id}/cancel")
    public CustomerOrder cancel(@PathVariable Long id, @RequestParam Long customerId) {
        return service.cancel(id, customerId);
    }

    @PatchMapping("/{id}/status")
    public CustomerOrder updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        try {
            return service.updateStatus(id, OrderStatus.valueOf(request.status().trim().toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "status must be SHIPPED or DELIVERED");
        }
    }

    @GetMapping("/analysis/summary")
    public Map<String, Object> analysis() { return service.analysis(); }

    public record OrderLineRequest(Long productId, Integer quantity) {}
    public record PlaceOrderRequest(Long customerId, List<OrderLineRequest> items, Long productId, Integer quantity, Double amount) {}
    public record StatusRequest(String status) {}
}
