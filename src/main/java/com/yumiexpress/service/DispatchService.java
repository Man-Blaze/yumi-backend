package com.yumiexpress.service;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.User;
import com.yumiexpress.repository.UserRepository;
import com.yumiexpress.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DispatchService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RoutingService routingService;

    // Max scooter capacity
    private static final double MAX_CAPACITY_KG = 15.0;

    // ==========================================
    // 1. MANUAL ASSIGNMENT (Used by Admin Portal)
    // ==========================================
    public User assignBestRider(Order newOrder) {
        // 1. Get all riders who are online and available
        List<User> availableRiders = userRepository.findAll().stream()
                .filter(user -> "RIDER".equals(user.getRole()) && user.isAvailable())
                .toList();

        if (availableRiders.isEmpty()) return null;

        User bestRider = null;
        double shortestDistance = Double.MAX_VALUE;

        for (User rider : availableRiders) {

            // 2. KNAPSACK CHECK: How much weight is this rider already carrying?
            List<Order> activeOrders = orderRepository.findAll().stream()
                    .filter(o -> o.getRider() != null
                            && o.getRider().getId().equals(rider.getId())
                            && ("ASSIGNED".equals(o.getStatus()) || "PICKED_UP".equals(o.getStatus())))
                    .toList();

            double currentLoad = activeOrders.stream().mapToDouble(Order::getWeightInKg).sum();

            // If adding this new order breaks the 15kg limit, skip this rider
            if ((currentLoad + newOrder.getWeightInKg()) > MAX_CAPACITY_KG) {
                continue;
            }

            // 3. TSP CHECK: How far is the rider from the pickup location?
            double distance = routingService.calculateDistance(
                    rider.getCurrentLat(), rider.getCurrentLng(),
                    newOrder.getPickupLat(), newOrder.getPickupLng()
            );

            // If this rider is closer than the previous best, make them the new best
            if (distance < shortestDistance) {
                shortestDistance = distance;
                bestRider = rider;
            }
        }

        return bestRider;
    }

    // ==========================================
    // 2. AUTO-DISPATCH ALGORITHM (Real-time Thread)
    // Runs every 10 seconds in the background
    // ==========================================
    @Scheduled(fixedRate = 10000)
    public void autoAssignRiders() {
        // Find all orders that need a rider.
        // We check that weight is greater than 0 so we don't assign unweighed B2B warehouse parcels.
        List<Order> pendingOrders = orderRepository.findAll().stream()
                .filter(o -> "AWAITING_PICKUP".equals(o.getStatus()) && o.getWeightInKg() > 0)
                .toList();

        for (Order order : pendingOrders) {
            // Use the exact same algorithm we built above
            User bestRider = assignBestRider(order);

            if (bestRider != null) {
                // 1. Assign the order
                order.setRider(bestRider);
                order.setStatus("ASSIGNED");
                orderRepository.save(order);

                // DO NOT SET TO BUSY.
                // The Knapsack logic already ensures they don't exceed 15kg.
                // We want them to keep getting orders until their bag is full.

                System.out.println("🤖 AUTO-DISPATCH: Assigned Order #" + order.getId() + " to Rider " + bestRider.getFullName());
            }
        }
    }
}