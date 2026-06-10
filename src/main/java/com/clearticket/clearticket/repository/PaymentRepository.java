package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
