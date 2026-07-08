package com.clearticket.clearticket.controller;

//import com.clearticket.clearticket.model.entity.Reservation;
import com.clearticket.clearticket.model.dto.*;
import com.clearticket.clearticket.service.PaymentService;
import com.clearticket.clearticket.service.ReservationService;
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
            @RequestBody ReservationRequestDto requestDto) {
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