package com.csci318.user.presentation;

import com.csci318.user.domain.User;
import com.csci318.user.service.UserProfile;
import com.csci318.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    public record RegisterRequest(String name, String email, String password) {}
    public record LoginRequest(String email, String password) {}

    // POST {"name","email","password"}  -> new user with role CUSTOMER
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@RequestBody RegisterRequest req) {
        return service.register(req.name(), req.email(), req.password());
    }

    // POST {"email","password"} -> the user, including their role (use the id for orders / profile)
    @PostMapping("/login")
    public User login(@RequestBody LoginRequest req) { return service.login(req.email(), req.password()); }

    // View profile: details + order summary
    @GetMapping("/{id}")
    public UserProfile viewProfile(@PathVariable Long id) { return service.viewProfile(id); }

    @GetMapping
    public List<User> all() { return service.all(); }
}
