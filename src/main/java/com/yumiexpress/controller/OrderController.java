package com.yumiexpress.controller;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.User;
import com.yumiexpress.service.DispatchService;
import com.yumiexpress.service.PricingService;
import com.yumiexpress.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DispatchService dispatchService; // Add this!

    @PostMapping("/book")
    public Order bookOrder(@RequestParam double weight, @RequestParam String senderName) {
        Order newOrder = new Order();
        newOrder.setWeightInKg(weight);
        newOrder.setStatus("PENDING");

        // 1. Calculate Price
        double price = pricingService.calculatePrice(weight);
        newOrder.setPrice(price);

        // 2. Find a Rider using the Dispatch Algorithm
        User assignedRider = dispatchService.assignBestRider(newOrder);
        if (assignedRider != null) {
            newOrder.setRider(assignedRider);
            newOrder.setStatus("ASSIGNED"); // Update status!
        }

        // 3. Save to the database
        return orderRepository.save(newOrder);
    }
}