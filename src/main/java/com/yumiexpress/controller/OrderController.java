package com.yumiexpress.controller;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.PaymentRecord;
import com.yumiexpress.model.User;
import com.yumiexpress.service.DispatchService;
import com.yumiexpress.service.PaymentService;
import com.yumiexpress.service.PricingService;
import com.yumiexpress.repository.OrderRepository;
import com.yumiexpress.repository.UserRepository;
import com.yumiexpress.util.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private QrCodeGenerator qrCodeGenerator; // <--- ADDED THIS

    @PostMapping("/book")
    public Order bookOrder(
            @RequestParam double weight,
            @RequestParam String senderName,
            @RequestParam double lat,
            @RequestParam double lng) {

        Order newOrder = new Order();
        newOrder.setWeightInKg(weight);
        newOrder.setStatus("AWAITING_PAYMENT");
        newOrder.setPickupAddress("Sender: " + senderName);
        newOrder.setPickupLat(lat);
        newOrder.setPickupLng(lng);
        newOrder.setPrice(pricingService.calculatePrice(weight));

        Order savedOrder = orderRepository.save(newOrder);

        PaymentRecord payment = paymentService.createPaymentRequest(savedOrder);
        savedOrder.setM_vatu_reference(payment.getM_vatu_reference());

        return orderRepository.save(savedOrder);
    }

    @PostMapping("/confirm-payment")
    public Order confirmPaymentAndDispatch(@RequestParam String referenceNumber) {
        var payment = paymentService.confirmPayment(referenceNumber);
        Order order = payment.getOrder();

        if (order.getTrackingId() == null) {
            String trackingId = "YE-" + (10000 + new java.util.Random().nextInt(90000));
            order.setTrackingId(trackingId);
        }

        User assignedRider = dispatchService.assignBestRider(order);
        if (assignedRider != null) {
            order.setRider(assignedRider);
            order.setStatus("ASSIGNED");
        } else {
            order.setStatus("PAID_AWAITING_RIDER");
        }

        return orderRepository.save(order);
    }

    @PutMapping("/{orderId}/status")
    public Order updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);

        if ("DELIVERED".equals(status)) {
            paymentService.processPayment(order);
        }

        return orderRepository.save(order);
    }

    @GetMapping("/rider/{riderId}/tasks")
    public List<Order> getRiderTasks(@PathVariable Long riderId) {
        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new RuntimeException("Rider not found"));
        return orderRepository.findByRider(rider);
    }

    // --- THIS IS THE MISSING QR CODE ENDPOINT ---
    @GetMapping("/{orderId}/qrcode")
    public ResponseEntity<byte[]> getOrderQrCode(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        // Create the rich JSON payload
        String qrPayload = String.format(
                "{\"trackingId\":\"%s\", \"orderId\":%d, \"sender\":\"%s\", \"rider\":\"%s\", \"weight\":\"%.1fkg\", \"price\":\"%.0fVT\", \"status\":\"%s\", \"date\":\"%s\"}",
                order.getTrackingId(),
                order.getId(),
                order.getPickupAddress(),
                order.getRider() != null ? order.getRider().getFullName() : "Unassigned",
                order.getWeightInKg(),
                order.getPrice(),
                order.getStatus(),
                java.time.LocalDateTime.now().toString()
        );

        byte[] image = qrCodeGenerator.generateQrCodeImage(qrPayload, 350, 350);

        return ResponseEntity.ok()
                .header("Content-Type", "image/png")
                .body(image);
    }
}