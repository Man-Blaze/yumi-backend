package com.yumiexpress.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String phoneNumber;
    private String password;
    private String role; // "CUSTOMER" or "RIDER"

    private String vehicleType;// "COMPANY_SCOOTER", "OWN_SCOOTER", "COMPANY_TRUCK", "OWN_CAR"
    private boolean available;
    // NEW FIELDS FOR RIDERS
    private double currentLat;
    private double currentLng;

    private String nationalId;
    private String area;

    // --- NEW FLEET & PAY FIELDS ---

    private double payRate = 0.0;     // e.g., 300 VT per delivery for own scooter, 150 VT for company scooter
}