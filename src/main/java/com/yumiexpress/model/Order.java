package com.yumiexpress.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "rider_id")
    private User rider;

    private String recipientPhone;
    private String pickupAddress;
    private String deliveryAddress;
    private double weightInKg;
    private double price;
    private String status;

    // ROUTING FIELDS
    private double pickupLat;
    private double pickupLng;
    private String m_vatu_reference;
    private String trackingId;

    // PROFILE & MOVEMENT FIELDS
    private String exactPickupLocation;
    private String pickupTime;

    // EMERGENCY & SAFETY FIELDS
    private boolean isEmergency = false;
    private String emergencyReason;

    // WAIT TIMER FIELDS
    private LocalDateTime riderArrivalTime;
    private LocalDateTime waitTimeExpiry;

    // --- TRACKING & RATING FIELDS ---
    private LocalDateTime estimatedArrival;
    private int customerRating = 0;
    private String customerFeedback;

    // --- BULK SELLER & ROUTING FIELDS ---
    private String sellerTrackingNumber;
    private String receiverAddress;
    private String specificAddressDetails;
    private String deliveryZone; // <--- ADDED THIS

    private String deliveryType; // "B2C", "B2B", "B2G"
    private String receiverEmail; // For future notifications
    private String senderAddress; // Full sender address
    private String receiverAddressFull; // Full receiver address
}