package com.csci318.customer.presentation;

import com.csci318.customer.domain.Customer;
import com.csci318.customer.service.CustomerProfile;
import com.csci318.customer.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) { this.service = service; }

    public record LoginRequest(String email, String password) {}

    // POST {"name","email","password","address"}
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Customer register(@RequestBody Customer customer) { return service.register(customer); }

    // POST {"email","password"} -> the customer (use its id for orders / profile)
    @PostMapping("/login")
    public Customer login(@RequestBody LoginRequest req) { return service.login(req.email(), req.password()); }

    // View profile: details + order summary
    @GetMapping("/{id}")
    public CustomerProfile profile(@PathVariable Long id) { return service.profile(id); }

    @GetMapping
    public List<Customer> all() { return service.all(); }
}
