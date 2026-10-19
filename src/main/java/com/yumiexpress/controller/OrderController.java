package com.yumiexpress.controller;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.User;
import com.yumiexpress.service.DispatchService;
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

    @PostMapping("/book")
    public Order bookOrder(
            @RequestParam double weight,
            @RequestParam String senderName,
            @RequestParam double lat,
            @RequestParam double lng) {

        Order newOrder = new Order();
        newOrder.setWeightInKg(weight);
        newOrder.setStatus("PENDING");

        // I added the senderName back in!
        newOrder.setPickupAddress("Sender: " + senderName);

        newOrder.setPickupLat(lat);
        newOrder.setPickupLng(lng);

        newOrder.setPrice(pricingService.calculatePrice(weight));

        User assignedRider = dispatchService.assignBestRider(newOrder);
        if (assignedRider != null) {
            newOrder.setRider(assignedRider);
            newOrder.setStatus("ASSIGNED");
        }

        return orderRepository.save(newOrder);
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