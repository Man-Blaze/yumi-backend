package com.yumiexpress.controller;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.User;
import com.yumiexpress.repository.OrderRepository;
import com.yumiexpress.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class FinanceController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/finance-dashboard")
    public String showFinanceDashboard(Model model) {
        List<Order> allOrders = orderRepository.findAll();

        // 1. CALCULATE REVENUE (Only count Delivered or Picked Up orders)
        double totalRevenue = allOrders.stream()
                .filter(o -> "DELIVERED".equals(o.getStatus()) || "PICKED_UP".equals(o.getStatus()))
                .mapToDouble(Order::getPrice)
                .sum();

        // 2. CALCULATE RIDER PAYOUTS
        double totalRiderPayouts = 0;
        List<User> riders = userRepository.findAll().stream()
                .filter(u -> "RIDER".equals(u.getRole()))
                .toList();

        for (User rider : riders) {
            // Count how many parcels this rider delivered
            long deliveries = allOrders.stream()
                    .filter(o -> o.getRider() != null && o.getRider().getId().equals(rider.getId())
                            && "DELIVERED".equals(o.getStatus()))
                    .count();

            // Pay = (Base Pay Rate x Deliveries)
            // Or if you use a fixed rate per delivery: deliveries * rider.getPayRate()
            double riderPay = deliveries * rider.getPayRate();
            totalRiderPayouts += riderPay;
        }

        // 3. ESTIMATE FUEL COSTS (Assuming 48 VT of fuel per delivery)
        long totalDeliveries = allOrders.stream().filter(o -> "DELIVERED".equals(o.getStatus())).count();
        double estimatedFuelCost = totalDeliveries * 48.0;

        // 4. CALCULATE NET PROFIT
        double netProfit = totalRevenue - totalRiderPayouts - estimatedFuelCost;

        // Send to HTML
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalRiderPayouts", totalRiderPayouts);
        model.addAttribute("estimatedFuelCost", estimatedFuelCost);
        model.addAttribute("netProfit", netProfit);
        model.addAttribute("totalDeliveries", totalDeliveries);

        return "finance-dashboard";
    }
}