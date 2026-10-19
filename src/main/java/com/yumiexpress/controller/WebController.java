package com.yumiexpress.controller;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.User;
import com.yumiexpress.repository.UserRepository;
import com.yumiexpress.service.DispatchService;
import com.yumiexpress.service.PaymentService;
import com.yumiexpress.service.PricingService;
import com.yumiexpress.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class WebController {

    @Autowired
    private PricingService pricingService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private DispatchService dispatchService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/book")
    public String showBookingForm() {
        return "book";
    }

    @PostMapping("/submit-booking")
    public String submitBooking(@RequestParam String senderName, @RequestParam double weight, Model model) {
        Order newOrder = new Order();
        newOrder.setWeightInKg(weight);
        newOrder.setPickupAddress("Sender: " + senderName);
        newOrder.setPrice(pricingService.calculatePrice(weight));
        newOrder.setStatus("AWAITING_PAYMENT");

        Order savedOrder = orderRepository.save(newOrder);

        // Auto-assign rider for the demo
        User assignedRider = dispatchService.assignBestRider(savedOrder);
        if (assignedRider != null) {
            savedOrder.setRider(assignedRider);
            savedOrder.setStatus("ASSIGNED");
            orderRepository.save(savedOrder);
        }

        model.addAttribute("order", savedOrder);
        return "success";
    }
    @Autowired
    private UserRepository userRepository;


    // 1. Show the Rider Dashboard page
    @GetMapping("/rider-portal/{riderId}")
    public String showRiderDashboard(@PathVariable Long riderId, Model model) {
        User rider = userRepository.findById(riderId).orElseThrow(() -> new RuntimeException("Rider not found"));

        // Find all tasks for this rider
        java.util.List<Order> tasks = orderRepository.findByRider(rider);

        model.addAttribute("rider", rider);
        model.addAttribute("tasks", tasks);
        return "rider-dashboard";
    }

    // 2. Handle the "Mark as Picked Up / Delivered" buttons
    @GetMapping("/rider/update-status/{id}")
    public String updateRiderStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);

        // Reload the dashboard
        return "redirect:/rider-portal/" + order.getRider().getId();
    }
    @GetMapping("/admin-portal")
    public String showAdminDashboard(Model model) {
        // Fetch ALL orders from the database
        java.util.List<Order> allOrders = orderRepository.findAll();

        // Sort them so the newest orders appear at the top
        allOrders.sort((a, b) -> b.getId().compareTo(a.getId()));

        model.addAttribute("allOrders", allOrders);
        return "admin";
    }
}