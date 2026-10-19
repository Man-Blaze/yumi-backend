package com.yumiexpress.controller;

import com.yumiexpress.model.User;
import com.yumiexpress.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public User registerUser(@RequestBody User newUser) {
        if (newUser.getRole() == null) {
            newUser.setRole("CUSTOMER");
        }
        return userRepository.save(newUser);
    }
}