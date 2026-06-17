package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.MyPageReservationResponseDto;
import com.clearticket.clearticket.model.dto.MyPageStatisticsResponseDto;
import com.clearticket.clearticket.model.dto.MyPageWaitingResponseDto;
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
    // 마이페이지 공통작업 백엔드 로직 구현
    // ==============================================================================

    /**
     * 로그인한 사용자의 상세 프로필 정보 조회
     * @param session
     * @return
     */
//    @GetMapping("/profile")
//    public ResponseEntity<?> getMyProfile(HttpSession session) {
//        Long userId = getLoginUserId(session);
//        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
//
//        // TODO: 서비스에 getUserProfile 메서드 만들기
//        Object profile = myPageService.getUserProfile(userId);
//        return ResponseEntity.ok(profile);
//    }

    /**
     * 프로필 수정에서 회원 정보 수정
     * @param updateData
     * @param session
     * @return
     */
//    @PutMapping("/profile")
//    public ResponseEntity<?> updateMyProfile(@RequestBody Map<String, Object> updateData, HttpSession session) {
//        Long userId = getLoginUserId(session);
//        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
//
//        try {
//            // TODO: 서비스에 updateUserProfile 메서드 만들기
//            myPageService.updateUserProfile(userId, updateData);
//            return ResponseEntity.ok("회원 정보가 성공적으로 수정되었습니다.");
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("수정에 실패했습니다: " + e.getMessage());
//        }
//    }

    /**
     * 배송지 목록 조회
     * @param session
     * @return
     */
//    @GetMapping("/addresses")
//    public ResponseEntity<?> getMyAddresses(HttpSession session) {
//        Long userId = getLoginUserId(session);
//        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
//
//        // TODO: 서비스에 getAddressList 메서드 만들기
//        List<?> addresses = myPageService.getAddressList(userId);
//        return ResponseEntity.ok(addresses);
//    }

    /**
     * 신규 배송지 등록
     * @param addressData
     * @param session
     * @return
     */
//    @PostMapping("/addresses")
//    public ResponseEntity<?> addAddress(@RequestBody Map<String, Object> addressData, HttpSession session) {
//        Long userId = getLoginUserId(session);
//        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
//
//        // TODO: 서비스에 addAddress 메서드 만들기
//        myPageService.addAddress(userId, addressData);
//        return ResponseEntity.ok("새 배송지가 등록되었습니다.");
//    }

    /**
     * 배송지 삭제
     * @param addressId
     * @param session
     * @return
     */
//    @DeleteMapping("/addresses/{address_id}")
//    public ResponseEntity<?> deleteAddress(@PathVariable("address_id") Long addressId, HttpSession session) {
//        Long userId = getLoginUserId(session);
//        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
//
//        // TODO: 서비스에 deleteAddress 메서드 만들기
//        myPageService.deleteAddress(addressId, userId);
//        return ResponseEntity.ok("배송지가 삭제되었습니다.");
//    }

    /**
     * 쿠폰 리스트 조회
     * @param session
     * @return
     */
//    @GetMapping("/coupons")
//    public ResponseEntity<?> getMyCoupons(HttpSession session) {
//        Long userId = getLoginUserId(session);
//        if (userId == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
//
//        // TODO: 서비스에 getCouponList 메서드 만들기
//        List<?> coupons = myPageService.getCouponList(userId);
//        return ResponseEntity.ok(coupons);
//    }

}