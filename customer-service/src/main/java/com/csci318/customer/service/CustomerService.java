package com.csci318.customer.service;

import com.csci318.customer.domain.Customer;
import com.csci318.customer.infrastructure.persistence.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Business rules for customers; the controller only handles HTTP
@Service
public class CustomerService {

    private final CustomerRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public CustomerService(CustomerRepository repo) { this.repo = repo; }

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

    public Customer profile(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found"));
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
