package com.csci318.customer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(unique = true)
    private String email;
    private String address;
    private LocalDateTime registeredAt;

    // Stores the BCrypt hash. WRITE_ONLY: accepted in requests, never returned in JSON responses
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    public Customer() {}

    public Customer(String name, String email, String address) {
        this.name = name;
        this.email = email;
        this.address = address;
    }

    @PrePersist
    void onRegister() { registeredAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
