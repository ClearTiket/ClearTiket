package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.*;
import com.clearticket.clearticket.model.entity.User;
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

    private Long getLoginUserId(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("loginUser");
        if (user == null) return null;
        return Long.parseLong(user.getId());
    }

    @GetMapping("/reservations/statistics")
    public ResponseEntity<?> getUserStatistics(HttpSession session) {
        Long userId = getLoginUserId(session);
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        MyPageStatisticsResponseDto statistics = myPageService.getUserStatistics(userId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/reservations")
    public ResponseEntity<?> getMyReservations(HttpSession session) {
        Long loginUser = getLoginUserId(session);
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        List<MyPageReservationResponseDto> reservations = myPageService.getMyReservations(loginUser);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/waitings")
    public ResponseEntity<?> getMyWaitings(HttpSession session) {
        Long loginUser = getLoginUserId(session);
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        List<MyPageWaitingResponseDto> waitings = myPageService.getMyWaitings(loginUser);
        return ResponseEntity.ok(waitings);
    }

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
     * - 형식/중복 검증 실패 시 400 + 명확한 메시지 반환
     * - 수정 성공 시 세션(loginUser)도 함께 갱신 (이메일/이름 변경 즉시 반영)
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateMyProfile(@RequestBody MyPageProfileUpdateRequestDto requestDto, HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            User updatedUser = myPageService.updateUserProfile(userId, requestDto);

            // 세션에 저장된 loginUser 정보도 최신화 (이메일/이름/전화번호 변경 반영)
            UserSession refreshedSession = new UserSession(
                    String.valueOf(updatedUser.getUserId()),
                    updatedUser.getName(),
                    updatedUser.getEmail(),
                    updatedUser.getPhone()
            );
            session.setAttribute("loginUser", refreshedSession);

            return ResponseEntity.ok("회원 정보가 성공적으로 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            // 검증/중복 오류 → 사용자에게 보여줘도 되는 메시지만 노출
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 예상치 못한 오류 → 내부 정보(e.getMessage()) 노출 금지
            return ResponseEntity.internalServerError().body("일시적인 오류로 수정에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    @GetMapping("/addresses")
    public ResponseEntity<?> getMyAddresses(HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        List<MyPageAddressResponseDto> addresses = myPageService.getAddressList(userId);

        return ResponseEntity.ok(addresses);
    }

    @PostMapping("/addresses")
    public ResponseEntity<?> addAddress(@RequestBody MyPageAddressSaveRequestDto requestDto, HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        myPageService.addAddress(userId, requestDto);

        return ResponseEntity.ok("새 배송지가 등록되었습니다.");
    }

    @PutMapping("/addresses/{address_id}")
    public ResponseEntity<?> updateAddress(@PathVariable("address_id") Long addressId,
                                           @RequestBody MyPageAddressSaveRequestDto requestDto,
                                           HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            myPageService.updateAddress(addressId, userId, requestDto);
            return ResponseEntity.ok("배송지가 수정되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/addresses/{address_id}")
    public ResponseEntity<?> deleteAddress(@PathVariable("address_id") Long addressId, HttpSession session) {
        Long userId = getLoginUserId(session);

        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        myPageService.deleteAddress(addressId, userId);

        return ResponseEntity.ok("배송지가 삭제되었습니다.");
    }

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