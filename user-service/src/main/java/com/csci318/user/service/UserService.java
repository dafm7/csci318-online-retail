package com.csci318.user.service;

import com.csci318.user.domain.User;
import com.csci318.user.infrastructure.client.OrderClient;
import com.csci318.user.infrastructure.client.OrderClient.OrderData;
import com.csci318.user.infrastructure.persistence.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

// Business rules for users; the controller only handles HTTP
@Service
public class UserService {

    private static final int RECENT_ORDERS = 5;

    private final UserRepository repo;
    private final OrderClient orderClient;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo, OrderClient orderClient) {
        this.repo = repo;
        this.orderClient = orderClient;
    }

    // Anyone who registers is a CUSTOMER (the User factory decides the role)
    public User register(String name, String email, String password) {
        if (isBlank(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
        }
        if (email != null && repo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }
        try {
            return repo.save(User.registerCustomer(name, email, encoder.encode(password)));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public void createAdminIfMissing(String name, String email, String password) {
        if (!repo.existsByEmail(email)) {
            repo.save(User.createAdmin(name, email, encoder.encode(password)));
            System.out.println("[user-service] admin account created: " + email);
        }
    }

    public User login(String email, String password) {
        return repo.findByEmail(email)
                .filter(u -> password != null && encoder.matches(password, u.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid email or password"));
    }

    // User details plus an order summary from order-service
    public UserProfile viewProfile(Long id) {
        User u = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        UserProfile.OrderHistory history = orderClient.ordersFor(id).map(this::summarise).orElse(null);
        return new UserProfile(u.getId(), u.getName(), u.getEmail(), u.getRole(), history);
    }

    public List<User> all() { return repo.findAll(); }

    private UserProfile.OrderHistory summarise(List<OrderData> orders) {
        List<OrderData> paid = orders.stream().filter(o -> "PAID".equals(o.status())).toList();
        double totalSpent = Math.round(paid.stream().mapToDouble(OrderData::amount).sum() * 100) / 100.0;
        List<UserProfile.RecentOrder> recent = orders.stream()
                .sorted(Comparator.comparing(OrderData::id).reversed())
                .limit(RECENT_ORDERS)
                .map(o -> new UserProfile.RecentOrder(o.id(), o.productId(), o.quantity(), o.amount(), o.status()))
                .toList();
        return new UserProfile.OrderHistory(orders.size(), paid.size(), totalSpent, recent);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
