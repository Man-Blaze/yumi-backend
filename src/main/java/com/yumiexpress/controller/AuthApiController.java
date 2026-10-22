package com.yumiexpress.controller;

import com.yumiexpress.model.User;
import com.yumiexpress.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> mobileLogin(@RequestBody Map<String, String> credentials) {
        String phone = credentials.get("phoneNumber");
        String nationalId = credentials.get("nationalId");

        User user = userRepository.findByPhoneNumber(phone).orElse(null);

        // Check if user exists and the password matches, OR if they registered via OTP
        if (user != null && (user.getPassword().equals(nationalId) || "OTP_VERIFIED".equals(user.getPassword()))) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("fullName", user.getFullName());
            response.put("role", user.getRole());
            response.put("area", user.getArea() != null ? user.getArea() : "Port Vila");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body("Invalid Phone or National ID");
    }

    // ADD THIS NEW ENDPOINT FOR FLUTTER OTP VERIFICATION
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyMobileOtp(@RequestBody Map<String, String> payload) {
        String phone = payload.get("phoneNumber");
        String otp = payload.get("otpCode");

        if (!"1234".equals(otp)) {
            return ResponseEntity.status(400).body("Invalid OTP");
        }

        User user = userRepository.findByPhoneNumber(phone).orElse(null);
        if (user == null) {
            user = new User();
            user.setPhoneNumber(phone);
            user.setFullName("Customer " + phone);
            user.setRole("CUSTOMER");
            user.setPassword("OTP_VERIFIED");
            user.setArea("Port Vila");
            userRepository.save(user);
        }

        // Return the user data so Flutter can log them in immediately
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("role", user.getRole());
        response.put("area", user.getArea());
        return ResponseEntity.ok(response);
    }
}