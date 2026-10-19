package com.yumiexpress.repository;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Find all orders assigned to a specific rider that are not yet delivered
    List<Order> findByRiderAndStatus(User rider, String status);

    // Find ALL orders for a rider (regardless of status)
    List<Order> findByRider(User rider);
}