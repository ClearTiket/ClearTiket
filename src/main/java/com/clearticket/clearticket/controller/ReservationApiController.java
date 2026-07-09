package com.clearticket.clearticket.controller;

//import com.clearticket.clearticket.model.entity.Reservation;
import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.*;
import com.clearticket.clearticket.service.PaymentService;
import com.clearticket.clearticket.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationApiController {

    private final ReservationService reservationService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> createReservation(
            @RequestBody ReservationRequestDto requestDto,
            HttpSession session) {

        // 화면(버튼 클릭, 페이지 진입)에서는 로그인 여부를 체크하고 있었지만,
        // 이 API 자체는 세션을 확인하지 않아 로그인 없이도 직접 호출하면 예매가 생성될 수 있었다.
        // 세션의 로그인 사용자와 요청받은 userId가 일치하는지까지 검증해 다른 사람 명의로
        // 예매가 생성되는 것도 함께 막는다.
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        if (requestDto.getUserId() == null || !loginUser.getId().equals(String.valueOf(requestDto.getUserId()))) {
            return ResponseEntity.status(403).body("본인 명의로만 예매할 수 있습니다.");
        }

        try {
            ReservationResponseDto response = reservationService.createReservation(requestDto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{reservation_id}")
    public ResponseEntity<ReservationResponseDto> getReservationById(
            @PathVariable("reservation_id") Long reservationId) {
        ReservationResponseDto response = reservationService.getReservationById(reservationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ReservationResponseDto>> getAllReservations() {
        List<ReservationResponseDto> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @PostMapping("/{reservation_id}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable("reservation_id") Long reservationId) {
        reservationService.cancelReservationById(reservationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reservation_id}/coupon")
    public ResponseEntity<String> applyCoupon(
            @PathVariable("reservation_id") Long reservationId,
            @RequestBody CouponApplyRequestDto dto) {

        try {
            Long couponId = "NONE".equals(dto.getCouponId()) ? null : Long.parseLong(dto.getCouponId());
            reservationService.applyCouponToReservation(reservationId, couponId, dto.getTotalPrice());
            return ResponseEntity.ok("쿠폰 정보가 성공적으로 반영되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("쿠폰 반영 실패: " + e.getMessage());
        }
    }

    @PutMapping("/{reservation_id}/buyer-info")
    public ResponseEntity<String> updateBuyerInfo(
            @PathVariable("reservation_id") Long reservationId,
            @RequestBody ReservationBuyerInfoRequestDto requestDto) {

        try {
            reservationService.updateBuyerInfo(reservationId, requestDto);
            return ResponseEntity.ok("주문자 정보가 성공적으로 저장되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{reservation_id}/payment")
    public ResponseEntity<PaymentResponseDto> processPayment(
            @PathVariable("reservation_id") Long reservationId,
            @RequestBody PaymentRequestDto paymentRequestDto) {

        log.info("결제 API 요청 수신 - 예매 ID: {}, 결제 방식: {}, 금액: {}원",
                reservationId, paymentRequestDto.getPaymentMethod(), paymentRequestDto.getAmount());

        paymentRequestDto.setReservationId(reservationId);
        PaymentResponseDto responseDto = paymentService.createPayment(paymentRequestDto);

        return ResponseEntity.ok(responseDto);
    }

}