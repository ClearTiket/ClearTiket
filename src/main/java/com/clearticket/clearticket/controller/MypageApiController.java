package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.*;
import com.clearticket.clearticket.service.MyPageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MypageApiController {

    private final MyPageService myPageService;

    /**
     * 세션에서 로그인한 사용자 정보 조회
     * @param session 현재 요청의 HTTP 세션
     * @return 로그인된 사용자 세션 객체, 미로그인 시 null
     */
    private Long getLoginUserId(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("loginUser");
        if (user == null) return null;
        return Long.parseLong(user.getId());
    }

    /**
     * 사용자 통계 및 취향 분석
     * 로그인 사용자의 총 예매 횟수 및 결제 금액 통계 데이터 조회
     * @param session 현재 요청의 HTTP 세션
     * @return 사용자 통계 데이터 객체, 미로그인 시 401 에러 반환
     */
    @GetMapping("/reservations/statistics")
    public ResponseEntity<?> getUserStatistics(HttpSession session) {
        Long userId = getLoginUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        MyPageStatisticsResponseDto statistics = myPageService.getUserStatistics(userId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 예매 내역 리스트 조회
     * @param session 현재 요청의 HTTP 세션
     * @return 사용자의 예매 내역 DTO 리스트, 미로그인 시 401 에러 반환
     */
    @GetMapping("/reservations")
    public ResponseEntity<?> getMyReservations(HttpSession session) {
        Long loginUser = getLoginUserId(session);
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        List<MyPageReservationResponseDto> reservations = myPageService.getMyReservations(loginUser);
        return ResponseEntity.ok(reservations);
    }

    /**
     * 내 예매대기 목록 조회
     * 사용자가 신청해 둔 모든 예매 대기 리스트 조회
     * @param session 현재 요청의 HTTP 세션
     * @return 사용자의 예매 대기 목록 DTO 리스트, 미로그인 시 401 에러 반환
     */
    @GetMapping("/waitings")
    public ResponseEntity<?> getMyWaitings(HttpSession session) {
        Long loginUser = getLoginUserId(session);
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        List<MyPageWaitingResponseDto> waitings = myPageService.getMyWaitings(loginUser);
        return ResponseEntity.ok(waitings);
    }

    /**
     * 예매 대기 취소 실행
     * 특정 예매 대기 신청을 취소하고 목록에서 제외
     * @param waitingId 취소할 예매 대기 고유 ID
     * @param session 현재 요청의 HTTP 세션
     * @return 취소 성공 메시지, 검증 실패 시 에러 메시지 반환
     */
    @PostMapping("/waitings/{waiting_id}/cancel")
    public ResponseEntity<?> cancelWaiting(@PathVariable("waiting_id") Long waitingId, HttpSession session) {
        Long loginUser = getLoginUserId(session);
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            myPageService.cancelWaitingById(waitingId, loginUser);
            return ResponseEntity.ok("예매 대기가 성공적으로 취소되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    // ==============================================================================
    // 마이페이지 회원정보 및 배송지관리
    // ==============================================================================

    /**
     * 로그인한 사용자의 상세 프로필 정보 조회
     * @param session 현재 요청 세션
     * @return 인증 성공시 프로필 정보, 미인증시 에러 메세지
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        MyPageProfileResponseDto profile = myPageService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * 프로필 수정에서 회원 정보 수정
     * @param requestDto 화면에서 전달된 프로필 수정 요청 데이터
     * @param session 현재 요청 세션
     * @return 수정 성공시 성공 메세지, 미인증시 에러 메세지
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateMyProfile(@RequestBody MyPageProfileUpdateRequestDto requestDto, HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            myPageService.updateUserProfile(userId, requestDto);
            return ResponseEntity.ok("회원 정보가 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("수정에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 배송지 목록 전체 조회
     * @param session 현재 요청 세션
     * @return 조회 성공시 배송지 정보 리스트, 미인증시 에러 메세지
     */
    @GetMapping("/addresses")
    public ResponseEntity<?> getMyAddresses(HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        List<MyPageAddressResponseDto> addresses = myPageService.getAddressList(userId);

        return ResponseEntity.ok(addresses);
    }

    /**
     * 신규 배송지 등록
     * @param requestDto 화면에서 전달된 신규 배송지 등록 요청 데이터
     * @param session 현재 요청 세션
     * @return 등록 성공시 성공 메세지, 미인증시 에러 메세지
     */
    @PostMapping("/addresses")
    public ResponseEntity<?> addAddress(@RequestBody MyPageAddressSaveRequestDto requestDto, HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        myPageService.addAddress(userId, requestDto);

        return ResponseEntity.ok("새 배송지가 등록되었습니다.");
    }

    /**
     * 배송지 삭제
     * @param addressId 삭제 배송지 ID
     * @param session 현재 요청 세션
     * @return 삭제 성공시 성공 메세지, 미인증시 에러 메세지
     */
    @DeleteMapping("/addresses/{address_id}")
    public ResponseEntity<?> deleteAddress(@PathVariable("address_id") Long addressId, HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        myPageService.deleteAddress(addressId, userId);

        return ResponseEntity.ok("배송지가 삭제되었습니다.");
    }

    /**
     * 쿠폰 목록 전체 조회
     * @param session 현재 요청 세션
     * @return 조회 성공 시 쿠폰 정보 리스트, 미인증시 에러 메세지
     */
    @GetMapping("/coupons")
    public ResponseEntity<?> getMyCoupons(HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        List<MyPageCouponResponseDto> coupons = myPageService.getCouponList(userId);

        return ResponseEntity.ok(coupons);
    }

}