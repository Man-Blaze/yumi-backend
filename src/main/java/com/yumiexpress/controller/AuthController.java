package com.yumiexpress.controller;

import com.yumiexpress.model.User;
import com.yumiexpress.repository.UserRepository;
import com.yumiexpress.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService; // Inject the token generator

    @PostMapping("/register")
    public User registerUser(@RequestBody User newUser) {
        if (newUser.getRole() == null) newUser.setRole("CUSTOMER");
        return userRepository.save(newUser);
    }

    // NEW: Login Endpoint
    @PostMapping("/login")
    public Map<String, String> loginUser(@RequestBody Map<String, String> credentials) {
        String phoneNumber = credentials.get("phoneNumber");
        String password = credentials.get("password");

        Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);

        if (userOptional.isPresent() && userOptional.get().getPassword().equals(password)) {
            User user = userOptional.get();

            // Generate the Token!
            String token = jwtService.generateToken(user.getPhoneNumber(), user.getRole());

            // Return it as a JSON response
            Map<String, String> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("role", user.getRole());
            response.put("id", String.valueOf(user.getId()));

            return response;
        } else {
            throw new RuntimeException("Invalid phone number or password");
        }
    }
}