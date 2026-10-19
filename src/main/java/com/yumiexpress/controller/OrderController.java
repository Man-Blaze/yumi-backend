package com.yumiexpress.controller;

import com.yumiexpress.model.Order;
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

    @PostMapping("/book")
    public Order bookOrder(@RequestParam double weight, @RequestParam String senderName) {
        Order newOrder = new Order();
        newOrder.setWeightInKg(weight);
        newOrder.setStatus("PENDING");

        // Calculate Price using your algorithm
        double price = pricingService.calculatePrice(weight);
        newOrder.setPrice(price);

        // Save to the database!
        return orderRepository.save(newOrder);
    }
}