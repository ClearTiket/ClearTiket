package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.*;
import com.clearticket.clearticket.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
     * @param payload 레몬스퀴지가 전송한 원본 JSON 맵
     * @return 결제 완료 처리 문구
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("[자동 연동] 레몬스퀴지 웹훅 신호 원본 수신 성공!");

        try {
            if (payload != null) {
                Map<String, Object> meta = (Map<String, Object>) payload.get("meta");
                if (meta == null) {
                    log.error("웹훅 데이터 내 [meta] 태그를 찾을 수 없습니다.");
                    return ResponseEntity.ok("WEBHOOK_OK");
                }

                String eventName = String.valueOf(meta.get("event_name"));

                Map<String, Object> customData = (Map<String, Object>) meta.get("custom_data");
                if (customData == null) {
                    log.error("웹훅 데이터 내 [custom_data] 태그를 찾을 수 없습니다.");
                    return ResponseEntity.ok("WEBHOOK_OK");
                }

                String resIdStr = String.valueOf(customData.get("reservation_id"));
                Long reservationId = Long.parseLong(resIdStr);

                log.info("추출 완료 -> 이벤트: {}, 예약 ID: {}", eventName, reservationId);

                LemonSqueezyWebhookDto.CustomData finalCustomData = new LemonSqueezyWebhookDto.CustomData(reservationId);
                LemonSqueezyWebhookDto.Meta finalMeta = new LemonSqueezyWebhookDto.Meta(finalCustomData);
                LemonSqueezyWebhookDto webhookDto = new LemonSqueezyWebhookDto(eventName, finalMeta);

                // 5. 알맹이가 꽉 찬 안전한 DTO를 서비스로 토스! 🔥
                paymentService.processWebhook(webhookDto);

                log.info("예약 번호 {}번의 DB 상태가 성공적으로 [CONFIRMED]로 변경되었습니다.", reservationId);
            }
        } catch (Exception e) {
            log.error("웹훅 처리 도중 서비스 내부에서 에러 발생: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok("WEBHOOK_OK");
    }

    /**
     * 결제 고유 ID 기반 단건 상세 조회
     * @param paymentId 조회할 결제 고유 ID
     * @return 특정 결제 상세 내역 객체
     */
    @GetMapping("/detail/{payment_id}")
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
     * @param paymentRequestDto 결제 수단, 입금 은행명, 결제 금액 및 예약 고유 ID를 포함한 요청 데이터 객체
     * @return 결제 완료 처리 정보 및 영수증 상세 데이터를 포함한 {@link ResponseEntity} 객체
     */
    @PostMapping("/bank-transfer")
    public ResponseEntity<PaymentResponseDto> processPayment(
            @RequestBody PaymentRequestDto paymentRequestDto) {
        PaymentResponseDto responseDto = paymentService.createPayment(paymentRequestDto);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 레몬스퀴지 결제 완료 후 팝업창 리다이렉트 주소 수신
     * @return 팝업창이 열리자마자 스스로를 무조건 강제 종료시키는 HTML 스크립트 코드
     */
    @GetMapping("/success")
    public ResponseEntity<String> handleLemonSqueezySuccessRedirect() {
        String selfCloseScript = "<script th:inline=\"javascript\">window.close();</script>";

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(selfCloseScript);
    }
}