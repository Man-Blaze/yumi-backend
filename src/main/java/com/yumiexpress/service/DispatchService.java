package com.yumiexpress.service;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.User;
import com.yumiexpress.repository.UserRepository;
import com.yumiexpress.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    // The maximum weight a scooter can carry
    private static final double MAX_SCOOTER_CAPACITY = 15.0;

    public User assignBestRider(Order newOrder) {
        List<User> availableRiders = userRepository.findAll().stream()
                .filter(user -> "RIDER".equals(user.getRole()) && user.isAvailable())
                .toList();

        if (availableRiders.isEmpty()) return null;

        User bestRider = null;
        double shortestDistance = Double.MAX_VALUE;

        for (User rider : availableRiders) {

            // 1. THE KNAPSACK CHECK: How much is this rider already carrying?
            double currentLoad = calculateCurrentLoad(rider);

            // 2. If the rider's current load + the new order exceeds 15kg, skip them!
            if ((currentLoad + newOrder.getWeightInKg()) > MAX_SCOOTER_CAPACITY) {
                continue;
            }

            // 3. THE TSP CHECK: If they have space, are they the closest?
            double distance = routingService.calculateDistance(
                    rider.getCurrentLat(), rider.getCurrentLng(),
                    newOrder.getPickupLat(), newOrder.getPickupLng()
            );

            if (distance < shortestDistance) {
                shortestDistance = distance;
                bestRider = rider;
            }
        }

        return bestRider;
    }

    // Helper method to calculate a rider's current weight
    private double calculateCurrentLoad(User rider) {
        // Find all orders assigned to this rider that are not yet delivered
        List<Order> activeOrders = orderRepository.findByRiderAndStatus(rider, "ASSIGNED");

        // Add up their weights
        return activeOrders.stream()
                .mapToDouble(Order::getWeightInKg)
                .sum();
    }
}