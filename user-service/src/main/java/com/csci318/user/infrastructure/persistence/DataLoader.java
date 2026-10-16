package com.csci318.user.infrastructure.persistence;

import com.csci318.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Creates the admin account on startup (H2 is in-memory, so it is recreated every run)
@Component
public class DataLoader implements CommandLineRunner {

    private final UserService service;
    private final String adminEmail;
    private final String adminPassword;

    public DataLoader(UserService service,
                      @Value("${admin.email}") String adminEmail,
                      @Value("${admin.password}") String adminPassword) {
        this.service = service;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        service.createAdminIfMissing("Admin", adminEmail, adminPassword);
    }
}
