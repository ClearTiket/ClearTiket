package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Coupon;
import com.clearticket.clearticket.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByUser(User user);
}