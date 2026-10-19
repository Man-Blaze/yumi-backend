package com.yumiexpress.controller;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.PaymentRecord;
import com.yumiexpress.model.User;
import com.yumiexpress.service.DispatchService;
import com.yumiexpress.service.PaymentService;
import com.yumiexpress.service.PricingService;
import com.yumiexpress.repository.OrderRepository;
import com.yumiexpress.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private UserRepository userRepository; // <--- THIS WAS MISSING

    @Autowired private PaymentService paymentService;

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

        // Calculate price
        newOrder.setPrice(pricingService.calculatePrice(weight));

        // 1. SAVE THE ORDER FIRST!
        Order savedOrder = orderRepository.save(newOrder);

        // 2. Now that the order has an ID, create the payment
        PaymentRecord payment = paymentService.createPaymentRequest(savedOrder);

        // 3. Attach the reference to the order and save it again
        savedOrder.setM_vatu_reference(payment.getM_vatu_reference());

        return orderRepository.save(savedOrder);
    }

    // This is the endpoint the app hits when the customer clicks "Pay" on their phone
    @PostMapping("/confirm-payment")
    public Order confirmPaymentAndDispatch(@RequestParam String referenceNumber) {

        // 1. Verify the payment via M-Vatu
        var payment = paymentService.confirmPayment(referenceNumber);

        // 2. Find the order linked to this payment
        Order order = payment.getOrder();

        // 3. NOW we dispatch the rider!
        User assignedRider = dispatchService.assignBestRider(order);
        if (assignedRider != null) {
            order.setRider(assignedRider);
            order.setStatus("ASSIGNED");
        } else {
            // If no rider is available, it stays pending, but at least it's paid!
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
        return orderRepository.save(order);
    }

    // Endpoint for a Rider to see their tasks
    @GetMapping("/rider/{riderId}/tasks")
    public List<Order> getRiderTasks(@PathVariable Long riderId) {

        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new RuntimeException("Rider not found"));

        return orderRepository.findByRider(rider);
    }
}