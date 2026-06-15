package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.LemonSqueezyRequestDto;
import com.clearticket.clearticket.model.dto.LemonSqueezyResponseDto;
import com.clearticket.clearticket.model.dto.PaymentRequestDto;
import com.clearticket.clearticket.model.dto.PaymentResponseDto;
import com.clearticket.clearticket.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

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
}
