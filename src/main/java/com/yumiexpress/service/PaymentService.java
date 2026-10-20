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

    // This is called when the rider marks the order as DELIVERED
    public void processPayment(Order order) {
        // 1. Find the existing payment record for this order
        PaymentRecord payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        // 2. If it's already marked as PAID (which it should be from the pre-payment step)
        // we just verify it and update the timestamp to show when the delivery was completed.
        if ("PAID".equals(payment.getStatus())) {
            payment.setPaymentTime(java.time.LocalDateTime.now()); // Update time to delivery completion
            paymentRepository.save(payment);
            System.out.println("✅ Delivery Complete: Payment already confirmed for Order #" + order.getId());
        } else {
            // Fallback just in case
            payment.setStatus("PAID");
            payment.setPaymentTime(java.time.LocalDateTime.now());
            paymentRepository.save(payment);
            System.out.println("⚠️ Payment was not pre-confirmed. Marking as PAID upon delivery for Order #" + order.getId());
        }
    }
}