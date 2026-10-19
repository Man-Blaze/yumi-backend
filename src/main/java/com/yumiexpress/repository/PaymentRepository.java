package com.yumiexpress.repository;

import com.yumiexpress.model.PaymentRecord;
import com.yumiexpress.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByOrder(Order order);
}