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
            System.out.println("★ 쿠폰디버깅: 세션에 로그인 유저가 없습니다! (미로그인 상태)");
            return ResponseEntity.status(401).build();
        }

        Optional<User> user = userRepository.findByEmail(loginUser.getId());

    // [★ 수정] 고유 번호 대신 세션에 담겨있는 유저의 진짜 이메일(loginUser.getId())을 서비스에 넘겨줍니다!
        String userEmail = loginUser.getId();
        System.out.println("★ 쿠폰디버깅: 서비스에 요청할 유저 이메일 = " + userEmail);

        // 2️⃣ [★ 로그 추가] 서비스에 들어가기 바로 직전 신호 확인
        System.out.println("★ 쿠폰디버깅: 이 ID로 서비스에 쿠폰 조회를 요청합니다...");
        List<CouponResponseDto> coupons = couponService.getAvailableCoupons(userEmail);

        // 3️⃣ [★ 로그 추가] 서비스가 최종적으로 돌려준 쿠폰 리스트에 뭐가 들었는지 확인
        System.out.println("★ 쿠폰디버깅: 서비스가 최종 리턴한 쿠폰 리스트 개수 = " + (coupons != null ? coupons.size() : "null"));
        System.out.println("★ 쿠폰디버깅: 리스트 내용물 = " + coupons);

        return ResponseEntity.ok(coupons);
    }
}