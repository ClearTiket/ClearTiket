package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    // 2. 일반 컬럼들
    @Column(nullable = false, length = 100)
    private String couponName; // 쿠폰 이름 (예: 신규회원 가입 축하 쿠폰)

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType; // 할인 종류 (예: AMOUNT(금액), PERCENT(비율))

    @Column(nullable = false)
    private int discountValue; // 할인 값 (예: 5000원 또는 10%)

    @Column(length = 255)
    private String usageCondition; // 사용 조건 (예: 3만원 이상 구매 시)

    @Column(nullable = false)
    private LocalDate expiryDate; // 쿠폰 유효기간 만료일 (YYYY-MM-DD)

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CouponStatus status; // 쿠폰 상태 (예: AVAILABLE, USED, EXPIRED)

    // 데이터 생성 일시
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 쿠폰을 사용했을 때 상태를 USED로 변경하는 메서드
    public void useCoupon() {
        this.status = CouponStatus.USED;
    }
}

enum DiscountType {
    AMOUNT,   // 고정 금액 할인 (예: 5000원 할인)
    PERCENT   // 정률 비율 할인 (예: 10% 할인)
}

enum CouponStatus {
    AVAILABLE, // 사용 가능
    USED,      // 사용 완료
    EXPIRED    // 기간 만료
}