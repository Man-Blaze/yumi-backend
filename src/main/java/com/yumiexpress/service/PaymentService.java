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
        // Find the payment record by the reference number
        PaymentRecord payment = paymentRepository.findAll().stream()
                .filter(p -> p.getM_vatu_reference().equals(referenceNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment reference not found"));

        if (payment.getStatus().equals("PAID")) {
            throw new RuntimeException("Payment already completed");
        }

        // Mark as paid
        payment.setStatus("PAID");
        payment.setPaymentTime(LocalDateTime.now());
        return paymentRepository.save(payment);
    }
}