package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Coupon;
import com.clearticket.clearticket.model.entity.CouponStatus;
import com.clearticket.clearticket.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * 회원이 보유한 모든 쿠폰 목록 조회
     * @param user 조회할 사용자
     * @return 회원이 보유한 전체 쿠폰 리스트
     */
    List<Coupon> findByUser(User user);

    /**
     * 회원이 보유한 쿠폰 중, 특정 상태(예: AVAILABLE)의 쿠폰 목록 조회
     *
     * @param user 조회할 사용자
     * @param couponStatus 조회할 쿠폰 상태 (CouponStatus.AVAILABLE)
     * @return 상태 조건에 맞는 쿠폰 리스트
     */
    List<Coupon> findByUserAndCouponStatus(Optional<User> user, CouponStatus couponStatus);

    @Query("SELECT c FROM Coupon c WHERE c.user.email = :email AND c.couponStatus = :status")
    List<Coupon> findByUser_EmailAndCouponStatus(@Param("email") String email, @Param("status") CouponStatus status);
}