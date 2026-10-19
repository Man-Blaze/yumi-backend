package com.yumiexpress.service;

import org.springframework.stereotype.Service;

@Service
public class PricingService {
    public double calculatePrice(double weightInKg) {
        if (weightInKg <= 0) throw new IllegalArgumentException("Weight must be > 0");
        else if (weightInKg <= 0.5) return 500.0;
        else if (weightInKg <= 3.0) return 950.0;
        else if (weightInKg <= 5.0) return 1200.0;
        else if (weightInKg <= 10.0) return 1800.0;
        else return -1.0;
    }
}