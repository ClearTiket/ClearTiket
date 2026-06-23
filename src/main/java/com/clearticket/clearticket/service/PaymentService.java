package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.*;
import com.clearticket.clearticket.model.entity.*;
import com.clearticket.clearticket.repository.PaymentRepository;
import com.clearticket.clearticket.repository.ReservationRepository;
import com.clearticket.clearticket.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final LemonSqueezyClient lemonSqueezyClient;

    /**
     * 레몬스퀴지 체크아웃 가상 결제창 URL 생성
     * @param lemonSqueezyRequestDto 가상 결제창 생성을 위한 요청 데이터
     * @return 레몬스퀴지 가상 결제창 접속 URL 주소 객체
     */
    @Transactional(readOnly = true)
    public LemonSqueezyResponseDto createCheckout(LemonSqueezyRequestDto lemonSqueezyRequestDto) {
        log.info("Lemon Squeezy 동적 체크아웃 URL 생성 프로세스 시작");

        Long reservationId = lemonSqueezyRequestDto.getReservationId();

        // 예약 데이터 및 연관 엔티티 검증 및 조회
        Reservation reservation = reservationRepository.findByReservationId(reservationId);
        if (reservation == null) {
            log.error("체크아웃 생성 실패: 존재하지 않는 예약 ID = {}", reservationId);
            throw new IllegalArgumentException("존재하지 않는 예약 번호입니다: " + reservationId);
        }

        // DB에 저장된 실제 결제 금액 추출
        int realPrice = reservation.getTotalPrice();

        // 결제창에 표시할 커스텀 공연 상품명 동적 구성
        String performanceTitle = "클리어티켓 공연 예매";
        if (reservation.getSchedule() != null && reservation.getSchedule().getPerformance() != null) {
            performanceTitle = reservation.getSchedule().getPerformance().getTitle(); // 공연 제목이 있다면 공연명 교체
        }
        String checkoutName = String.format("[%s] %s 티켓", performanceTitle, reservation.getTicketType());

        log.info("DB 데이터 조회 완료 - 상품명: '{}', 결제 요청 금액: {}원", checkoutName, realPrice);

        // 레몬스퀴지 외부 API 연동 객체를 통한 실시간 체크아웃 URL 발급 요청
        String realCheckoutUrl = lemonSqueezyClient.requestDynamicCheckoutUrl(realPrice, checkoutName, reservationId);

        return new LemonSqueezyResponseDto(realCheckoutUrl);
    }


    /**
     * 레몬스퀴지로 부터 전달 받은 웹훅 신호 처리
     * 전달 받은 데이터 결제상태, 예매번호(ID) 확인 후
     * 결제상태가 "order_created" 인 경우 예약 상태를 "CONFIRMED" 로 변경 후 DB 저장
     * @param webhookDto 레몬스퀴지 웹훅이 보내준 데이터
     */
    public void processWebhook(LemonSqueezyWebhookDto webhookDto) {
        log.info("레몬스퀴지 웹훅 처리 시작");

        String eventName = webhookDto.getEventName();
        Long reservationId = webhookDto.getMeta().getCustomData().getReservationId();

        log.info("웹훅 분석 완료 → 결제상태: {}, 예매번호(ID): {}", eventName, reservationId);

        // 결제 성공시 "order_created"
        if ("order_created".equals(eventName)) {
            log.info("결제 성공 확인! 예매번호(ID) {}번을 [예매 완료] 상태로 업데이트", reservationId);
            Reservation reservation = reservationRepository.findByReservationId(reservationId);
             if (reservation != null) {
                reservation.changeStatus(ReservationStatus.CONFIRMED);
                reservationRepository.save(reservation);
                log.info("예매번호(ID) {}번 DB 상태 변경 완료 [CONFIRMED]", reservationId);
             } else {
                log.error("DB에서 예매번호 {}번을 찾을 수 없습니다!", reservationId);
             }
        }
    }


    /**
     * 레몬스퀴지에서 결제 완료 알림 처리
     * 사용자가 가상 결제창에서 카드 결제시, 레몬스퀴지 → 자바 서버 신호보냄
     * 스프링부트 DB에 최종 결제 내역을 저장하는 서비스
     * @param paymentRequestDto 레몬스퀴지에서 전달한 결제 데이터
     * @return DB에 성공적으로 저장된 최동 결제 내역(프론트 확인용)
     */
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto paymentRequestDto) {
        Payment payment = new Payment();

        payment.setAmount(paymentRequestDto.getAmount());
        payment.setBankName(paymentRequestDto.getBankName());

        Reservation reservation = null;

        // 결제 데이터와 예약 엔티티 간의 연관 관계 매핑
        if (paymentRequestDto.getReservationId() != null) {
            reservation = reservationRepository.findByReservationId(paymentRequestDto.getReservationId());
            payment.setReservation(reservation);
        }

        // 결제 수단 데이터 검증 및 예외 처리
        String methodStr = paymentRequestDto.getPaymentMethod();
        if (methodStr == null || methodStr.trim().isEmpty()) {
            methodStr = "CARD";
        }
        payment.setMethod(PaymentMethod.valueOf(methodStr.toUpperCase()));
        payment.setStatus(PaymentStatus.CONFIRMED);

        if ("BANK_TRANSFER".equalsIgnoreCase(methodStr) && reservation != null) {
            reservation.changeStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation); // 예약 상태 반영
            log.info("무통장 입금 확인: 예매번호 {}번 [CONFIRMED] 상태 변경 완료", reservation.getReservationId());
        }

        Payment savedPayment = paymentRepository.save(payment);

        return toResponseDto(savedPayment);
    }


    /**
     * 결제 고유 ID 기반 단건 내역 상세 조회
     * @param paymentId 조회할 결제 고유 ID
     * @return 특정 결제 내역 및 연관 공연 정보가 포함된 영수증 객체
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(Long paymentId) {
        Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);

        if(optionalPayment.isEmpty()) {
            throw new IllegalArgumentException("해당 결제 내역이 존재하지 않습니다. ID: " + paymentId);
        }

        Payment payment = optionalPayment.get();
        return toResponseDto(payment);
    }

    /**
     * 결제 전체 내역 목록 조회
     * @return 전체 결제 영수증 객체 리스트
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();

        List<PaymentResponseDto> result = new ArrayList<>();
        for (Payment payment : payments) {
            result.add(toResponseDto(payment));
        }

        return result;
    }


    /**
     * 특정 결제 내역 취소 및 상태 변경
     * @param paymentId 취소할 결제 고유 ID
     * @return 상태가 CANCELED로 변경된 최종 결제 영수증 객체
     */
    @Transactional
    public PaymentResponseDto cancelPayment(Long paymentId) {
        Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);
        if(optionalPayment.isEmpty()) {
            throw new IllegalArgumentException("해당 결제 내역이 존재하지 않습니다. ID: " + paymentId);
        }

        Payment payment = optionalPayment.get();
        payment.setStatus(PaymentStatus.CANCELED);

        Payment savedPayment = paymentRepository.save(payment);
        return toResponseDto(savedPayment);
    }


    /**
     * 결제 엔티티 데이터를 응답 DTO 객체로 변환
     * @param payment 변환할 객체 엔티티 원본 데이터
     * @return 결제 정보 및 연관 공연 제목이 포함된 최종 응답 DTO 객체
     */
    private PaymentResponseDto toResponseDto(Payment payment) {
        Reservation reservation = new Reservation();

        Schedule schedule = (reservation != null) ? reservation.getSchedule() : null;
        Performance performance = (schedule != null) ? schedule.getPerformance() : null;

        return new PaymentResponseDto(
                payment.getPaymentId(),
                reservation != null ? reservation.getReservationId() : null,
                performance != null ? performance.getTitle() : "공연 정보 없음",
                payment.getMethod() != null ? payment.getMethod().name() : null,
                payment.getBankName(),
                payment.getAmount(),
                payment.getStatus() != null ? payment.getStatus().name() : null,
                payment.getPaymentDate()
        );
    }

}
