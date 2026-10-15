package com.csci318.customer.service;

import com.csci318.customer.domain.Customer;
import com.csci318.customer.infrastructure.client.OrderClient;
import com.csci318.customer.infrastructure.client.OrderClient.OrderData;
import com.csci318.customer.infrastructure.persistence.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

// Business rules for customers; the controller only handles HTTP
@Service
public class CustomerService {

    private static final int RECENT_ORDERS = 5;

    private final CustomerRepository repo;
    private final OrderClient orderClient;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public CustomerService(CustomerRepository repo, OrderClient orderClient) {
        this.repo = repo;
        this.orderClient = orderClient;
    }

    public Customer register(Customer customer) {
        if (isBlank(customer.getName()) || isBlank(customer.getEmail()) || isBlank(customer.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name, email and password are required");
        }
        if (repo.existsByEmail(customer.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }
        customer.setPassword(encoder.encode(customer.getPassword()));
        return repo.save(customer);
    }

    public Customer login(String email, String password) {
        return repo.findByEmail(email)
                .filter(c -> password != null && encoder.matches(password, c.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password"));
    }

    // Customer details plus an order summary from order-service
    public CustomerProfile profile(Long id) {
        Customer c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found"));
        CustomerProfile.OrderHistory history = orderClient.ordersFor(id).map(this::summarise).orElse(null);
        return new CustomerProfile(c.getId(), c.getName(), c.getEmail(), c.getAddress(), c.getRegisteredAt(), history);
    }

    public List<Customer> all() { return repo.findAll(); }

    private CustomerProfile.OrderHistory summarise(List<OrderData> orders) {
        List<OrderData> paid = orders.stream().filter(o -> "PAID".equals(o.status())).toList();
        double totalSpent = Math.round(paid.stream().mapToDouble(OrderData::amount).sum() * 100) / 100.0;
        List<CustomerProfile.RecentOrder> recent = orders.stream()
                .sorted(Comparator.comparing(OrderData::id).reversed())
                .limit(RECENT_ORDERS)
                .map(o -> new CustomerProfile.RecentOrder(o.id(), o.productId(), o.quantity(), o.amount(), o.status()))
                .toList();
        return new CustomerProfile.OrderHistory(orders.size(), paid.size(), totalSpent, recent);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
