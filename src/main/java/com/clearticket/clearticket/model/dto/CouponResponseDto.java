package com.clearticket.clearticket.model.dto;

import com.clearticket.clearticket.model.entity.Coupon;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CouponResponseDto {
    private Long couponId;
    private String couponName;
    private String discountType;
    private int discountValue;
    private String couponStatus; // 쿠폰 상태 (AVAILABLE, USED, EXPIRED)

    /**
     * Coupon 엔티티 객체를 CouponResponseDto로 변환하기 위한 생성자
     * @param coupon 변환할 원본 쿠폰 엔티티 객체
     */
    public CouponResponseDto(Coupon coupon) {
        this.couponId = coupon.getCouponId();
        this.couponName = coupon.getCouponName();
        this.discountType = coupon.getDiscountType().name();
        this.discountValue = coupon.getDiscountValue();
        this.couponStatus = coupon.getCouponStatus().name();
    }
}