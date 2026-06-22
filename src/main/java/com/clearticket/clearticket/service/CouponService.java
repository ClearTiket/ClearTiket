package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.CouponResponseDto;
import com.clearticket.clearticket.model.entity.*;
import com.clearticket.clearticket.repository.CouponRepository;
import com.clearticket.clearticket.repository.ReservationRepository;
import com.clearticket.clearticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * 회원이 보유한 쿠폰 중,
     * 현재 사용 가능한(AVAILABLE) 쿠폰 목록을 조회하여 DTO 리스트로 반환
     * @param email 현재 로그인한 사용자 객체
     * @return 사용 가능한 CouponResponseDto 리스트
     */
        public List<CouponResponseDto> getAvailableCoupons(String email) {
            List<Coupon> coupons = couponRepository.findByUser_EmailAndCouponStatus(email, CouponStatus.AVAILABLE);

            List<CouponResponseDto> dtoList = new ArrayList<>();
            for (Coupon coupon : coupons) {
                dtoList.add(new CouponResponseDto(coupon));
            }
            return dtoList;
        }
}