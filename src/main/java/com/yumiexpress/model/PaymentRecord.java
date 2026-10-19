package com.yumiexpress.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private double amount;
    private String m_vatu_reference; // The simulated reference number
    private String status; // "PENDING", "PAID", "FAILED"
    private LocalDateTime paymentTime;
}