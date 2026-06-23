package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyPageCouponResponseDto {
    private Long couponId; // 쿠폰 고유 번호
    private String couponName; // 쿠폰 이름 (예: 신규 회원 3000원 할인권)
    private int discountValue; // 할인 금액 또는 할인율 (예: 3000)
    private String discountType; // 할인 타입 (예: AMOUNT, PERCENT)
    private LocalDateTime expiredAt; // 만료일
    private String couponStatus; // 상태 (AVAILABL:사용가능, USED:사용완료, EXPIRED:기간만료)
}