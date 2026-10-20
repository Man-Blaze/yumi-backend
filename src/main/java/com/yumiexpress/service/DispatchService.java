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

    // Max scooter capacity
    private static final double MAX_CAPACITY_KG = 15.0;

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
            // We must check both ASSIGNED (in their bag) and PICKED_UP (on the road)
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
}