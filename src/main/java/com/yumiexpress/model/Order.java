package com.yumiexpress.model;

import jakarta.persistence.*;
import lombok.Data;

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
    // NEW FIELDS FOR ROUTING
    private double pickupLat;
    private double pickupLng;

    private String m_vatu_reference;
}