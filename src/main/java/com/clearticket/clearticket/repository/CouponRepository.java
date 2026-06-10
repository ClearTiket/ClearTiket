package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}