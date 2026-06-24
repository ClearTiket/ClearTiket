package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.LemonSqueezyRequestDto;
import com.clearticket.clearticket.model.dto.LemonSqueezyResponseDto;
import com.clearticket.clearticket.model.dto.PaymentRequestDto;
import com.clearticket.clearticket.model.dto.PaymentResponseDto;
import com.clearticket.clearticket.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;

    /**
     * 레몬스퀴지 체크아웃 생성
     * @param lemonSqueezyRequestDto 가상 결제창 생성을 위한 요청 데이터
     * @return 레몬스퀴지 가상 결제창 접속 URL 주소 객체
     */
    @PostMapping("/checkout")
    public ResponseEntity<LemonSqueezyResponseDto> createCheckout(
            @RequestBody LemonSqueezyRequestDto lemonSqueezyRequestDto) {
        LemonSqueezyResponseDto responseDto = paymentService.createCheckout(lemonSqueezyRequestDto);
        return ResponseEntity.ok(responseDto);
    }


    /**
     * 레몬스퀴지 결제 완료 알림 수신 (웹훅)
     * @param paymentRequestDto 레몬스퀴지에서 전송한 결제 결과 데이터
     * @return 결제 완료 처리된 최종 영수증 객체
     */
    @PostMapping("/webhook")
    public ResponseEntity<PaymentResponseDto> handleWebhook(
            @RequestBody PaymentRequestDto paymentRequestDto) {
        // 레몬스퀴지가 보낸 데이터를 서비스에 넘겨서 DB에 영수증 저장
        PaymentResponseDto responseDto = paymentService.createPayment(paymentRequestDto);
        return ResponseEntity.ok(responseDto);
    }


    /**
     * 결제 고유 ID 기반 단건 상세 조회
     * @param paymentId 조회할 결제 고유 ID
     * @return 특정 결제 상세 내역 객체
     */
    @GetMapping("/{payment_id}")
    public ResponseEntity<PaymentResponseDto> getPaymentById(
        @PathVariable("payment_id") Long paymentId) {
        PaymentResponseDto response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }


    /**
     * 전체 결제 내역 목록 조회
     * @return 전체 결제 영수증 객체 리스트
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments() {
        List<PaymentResponseDto> response = paymentService.getAllPayments();
        return ResponseEntity.ok(response);
    }


    /**
     * 특정 결제 내역 취소
     * @param paymentId 취소할 결제 고유 ID
     * @return 취소 처리된 결제 영수증 객체
     */
    @PatchMapping("/{payment_id}/cancel")
    public ResponseEntity<PaymentResponseDto> cancelPayment(@PathVariable("payment_id") Long paymentId) {
        PaymentResponseDto response = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(response);
    }


    /**
     * 최종 결제 요청 처리 및 예약 상태 연동 API
     * 선택된 결제 수단(BANK_TRANSFER)에 따라 결제 내역을 저장하고
     * 무통장 입금일 경우 해당 예약의 상태를 즉시 확정(CONFIRMED) 상태로 업데이트합니다.
     * @param paymentRequestDto 결제 수단, 입금 은행명, 결제 금액 및 예약 고유 ID를 포함한 요청 데이터 객체
     * @return 결제 완료 처리 정보 및 영수증 상세 데이터를 포함한 {@link ResponseEntity} 객체
     */
    @PostMapping("/bank-transfer")
    public ResponseEntity<PaymentResponseDto> processPayment(
            @RequestBody PaymentRequestDto paymentRequestDto) {

        log.info("무통장 입금(BANK_TRANSFER) 결제 요청 수신 - 예약 ID: {}, 결제 방식: {}, 금액: {}원",
                paymentRequestDto.getReservationId(),
                paymentRequestDto.getPaymentMethod(),
                paymentRequestDto.getAmount());

        PaymentResponseDto responseDto = paymentService.createPayment(paymentRequestDto);

        return ResponseEntity.ok(responseDto);
    }

}
