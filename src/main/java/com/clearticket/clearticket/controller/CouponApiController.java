package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.CouponResponseDto;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.service.CouponService;
import com.clearticket.clearticket.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponApiController {

    private final CouponService couponService;
    private final UserRepository userRepository;

    /**
     * 현재 세션에 로그인된 사용자가 보유한 쿠폰 중, 사용 가능한(AVAILABLE) 쿠폰 목록을 조회
     * 프론트엔드 예매 팝업창에서 쿠폰 선택 목록을 동적으로 그릴 때 사용
     * @param session 현재 요청의 HTTP 세션
     * @return 사용 가능한 쿠폰 DTO 리스트를 포함한 ResponseEntity 미로그인시 에러
     */
    @GetMapping
    public ResponseEntity<List<CouponResponseDto>> getAvailableCoupons(HttpSession session) {
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");

        if (loginUser == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<User> user = userRepository.findByEmail(loginUser.getId());

        if (user.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 회원 정보입니다.");
        }

        List<CouponResponseDto> coupons = couponService.getAvailableCoupons(String.valueOf(user));
        return ResponseEntity.ok(coupons);
    }
}