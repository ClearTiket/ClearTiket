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

    /**
     * 예매 및 결제 생성
     * @param requestDto
     * @return
     */
    @PostMapping
    public ResponseEntity<?> createReservation(
            @RequestBody ReservationRequestDto requestDto) {
        try {
            ReservationResponseDto response = reservationService.createReservation(requestDto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 예매 상세 조회
     * @param reservationId 조회할 예약 ID
     * @return
     */
    @GetMapping("/{reservation_id}")
    public ResponseEntity<ReservationResponseDto> getReservationById(
            @PathVariable("reservation_id") Long reservationId) {
        ReservationResponseDto response = reservationService.getReservationById(reservationId);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 예약 내역 목록 조회
     * @return 조회된 예약 정보 응답 DTO 객체들이 담긴 리스트
     */
    @GetMapping("/all")
    public ResponseEntity<List<ReservationResponseDto>> getAllReservations() {
        List<ReservationResponseDto> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    /**
     * 예매 취소 실행
     * @param reservationId 취소할 예약 ID
     * @return
     */
    @PostMapping("/{reservation_id}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable("reservation_id") Long reservationId) {
        reservationService.cancelReservationById(reservationId);
        return ResponseEntity.ok().build();
    }


    /**
     * 예매 1단계 완료 : 선택한 쿠폰 및 최종 금액 임시 저장
     * @param reservationId 현재 진행 중인 예약 ID
     * @param dto 화면에서 전송된 쿠폰 ID 및 할인 적용 후 최종 결제 금액
     * @return 성공/실패 메세지
     */
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


    /**
     * 예매 2단계 : 수령/주문자 정보 저장
     * @param reservationId 현재 진행 중인 예약 ID
     * @param requestDto 화면에서 JSON 으로 넘어온 주문자 정보 데이터
     * @return
     */
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


    /**
     * 예매 3단계 : 최종 결제 요청 처리 API
     * @param reservationId 현재 진행 중인 예매 고유 번호
     * @param paymentRequestDto 프론트엔드 화면에서 전송된 결제 수단 및 금액 정보 주머니
     * @return 성공 시 저장된 영수증 정보 데이터 반환
     */
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