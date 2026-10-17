package com.csci318.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

// Aggregate root of the user management context.
// Created only through the factory methods below, so every user starts in a valid state.
// Table named "users" because USER is a reserved word in SQL
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(unique = true)
    private String email;

    @JsonIgnore   // BCrypt hash, never returned in JSON responses
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    protected User() {}   // for JPA only

    private User(String name, String email, String passwordHash, Role role) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is required");
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("password is required");
        this.name = name;
        this.email = email;
        this.password = passwordHash;
        this.role = role;
    }

    // Factory: anyone who registers themselves is a CUSTOMER
    public static User registerCustomer(String name, String email, String passwordHash) {
        return new User(name, email, passwordHash, Role.CUSTOMER);
    }

    // Factory: admin accounts are only created by the system (see DataLoader)
    public static User createAdmin(String name, String email, String passwordHash) {
        return new User(name, email, passwordHash, Role.ADMIN);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
}
