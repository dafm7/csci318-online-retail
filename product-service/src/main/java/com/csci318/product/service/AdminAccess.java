package com.csci318.product.service;

import com.csci318.product.infrastructure.client.UserClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

// Checks with user-service that the caller is an ADMIN before admin-only actions
@Service
public class AdminAccess {

    private final UserClient users;

    public AdminAccess(UserClient users) { this.users = users; }

    public void requireAdmin(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required (log in first and send your user id)");
        }
        UserClient.UserData user;
        try {
            user = users.find(userId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user " + userId + " does not exist"));
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "user-service is unavailable, cannot check admin role");
        }
        if (!"ADMIN".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only admins can do this (" + user.name() + " is " + user.role() + ")");
        }
    }
}
