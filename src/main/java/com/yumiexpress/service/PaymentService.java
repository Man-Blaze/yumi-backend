package com.yumiexpress.service;

import com.yumiexpress.model.PaymentRecord;
import com.yumiexpress.model.Order;
import com.yumiexpress.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    // 1. Generate the payment request before the rider takes off
    public PaymentRecord createPaymentRequest(Order order) {
        PaymentRecord payment = new PaymentRecord();
        payment.setOrder(order);
        payment.setAmount(order.getPrice());
        payment.setStatus("AWAITING_PAYMENT");

        int referenceNumber = 100000 + new Random().nextInt(900000);
        payment.setM_vatu_reference("MV-" + referenceNumber); // The customer uses this code to pay

        return paymentRepository.save(payment);
    }

    // 2. Simulate the customer approving the payment on their phone
    public PaymentRecord confirmPayment(String referenceNumber) {
        // Safety check: Make sure the reference number is not empty
        if (referenceNumber == null || referenceNumber.trim().isEmpty()) {
            throw new RuntimeException("Invalid M-Vatu Reference: Reference is empty.");
        }

        // Find the payment record by the reference number
        PaymentRecord payment = paymentRepository.findAll().stream()
                .filter(p -> p.getM_vatu_reference() != null && p.getM_vatu_reference().equals(referenceNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment reference not found: " + referenceNumber));

        if ("PAID".equals(payment.getStatus())) {
            throw new RuntimeException("Payment already completed for reference: " + referenceNumber);
        }

        // Mark as paid
        payment.setStatus("PAID");
        payment.setPaymentTime(java.time.LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    // 2. Process the payment when the receiver signs off
    public void processPayment(Order order) {
        // Find the payment record for this order
        PaymentRecord payment = paymentRepository.findByOrder(order).orElse(null);

        if (payment == null) {
            // B2C order: Create a record on the fly (it was paid at the door)
            payment = new PaymentRecord();
            payment.setOrder(order);
            payment.setAmount(order.getPrice());
            payment.setStatus("PAID");
            payment.setPaymentTime(java.time.LocalDateTime.now());
            payment.setM_vatu_reference(order.getM_vatu_reference() != null ? order.getM_vatu_reference() : "CASH-" + (1000 + new java.util.Random().nextInt(9000)));
            paymentRepository.save(payment);
            System.out.println("💵 B2C Payment processed at delivery for Order #" + order.getId());
        } else {
            // B2B order: Update the existing record (pre-paid)
            payment.setStatus("PAID");
            payment.setPaymentTime(java.time.LocalDateTime.now());
            paymentRepository.save(payment);
            System.out.println("✅ B2B Payment finalized for Order #" + order.getId());
        }
    }
}