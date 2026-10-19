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

    private String vehicleType;
    private boolean available;
    // NEW FIELDS FOR RIDERS
    private double currentLat;
    private double currentLng;
}