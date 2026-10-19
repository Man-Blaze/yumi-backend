package com.yumiexpress.service;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.User;
import com.yumiexpress.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DispatchService {

    @Autowired
    private UserRepository userRepository;

    // This is the Knapsack + Assignment logic
    public User assignBestRider(Order order) {
        // 1. Get all riders who are online and available
        List<User> availableRiders = userRepository.findAll().stream()
                .filter(user -> "RIDER".equals(user.getRole()) && user.isAvailable())
                .toList();

        if (availableRiders.isEmpty()) {
            return null; // No riders available
        }

        User bestRider = null;
        double lowestCurrentLoad = Double.MAX_VALUE;

        // 2. Find the rider with the lowest current load (Knapsack heuristic)
        // For now, we just pick the first available rider.
        // Later, we will calculate actual distance and weight capacity.
        for (User rider : availableRiders) {
            // In a full build, we would check rider.getCurrentWeight() + order.getWeight() <= 15kg
            bestRider = rider;
            break;
        }

        return bestRider;
    }
}