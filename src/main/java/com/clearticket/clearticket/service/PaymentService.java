package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.LemonSqueezyRequestDto;
import com.clearticket.clearticket.model.dto.LemonSqueezyResponseDto;
import com.clearticket.clearticket.model.dto.PaymentRequestDto;
import com.clearticket.clearticket.model.dto.PaymentResponseDto;
import com.clearticket.clearticket.model.entity.*;
import com.clearticket.clearticket.repository.PaymentRepository;
import com.clearticket.clearticket.repository.ReservationRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 레몬스퀴지 체크아웃 가상 결제창 URL 생성
     * @param lemonSqueezyRequestDto 가상 결제창 생성을 위한 요청 데이터
     * @return 레몬스퀴지 가상 결제창 접속 URL 주소 객체
     */
    public LemonSqueezyResponseDto createCheckout(LemonSqueezyRequestDto lemonSqueezyRequestDto) {
        // 테스트용 레몬스퀴지 가상 체크아웃 주소 생성 (실제 연동 전 테스트용 가짜 URL)
        String mockCheckoutUrl = "https://clearticket.lemonsqueezy.com/checkout/buy/mock-test-id?reservation_id="
                + lemonSqueezyRequestDto.getReservationId();

        return new LemonSqueezyResponseDto(mockCheckoutUrl);
    }

    /**
     * 신규 결제 요청 처리 및 완료 영수증 발급
     * @param paymentRequestDto 프론트엔드 결제 요청 데이터 (금액, 결제수단 등)
     * @return 결제 완료 내역 및 연관 공연 정보가 포함된 최종 영수증 객체
     */
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto paymentRequestDto) {
        Payment payment = new Payment();

        payment.setAmount(paymentRequestDto.getAmount());
        payment.setBankName(paymentRequestDto.getBankName());

        payment.setPaymentMethod(PaymentMethod.valueOf(paymentRequestDto.getPaymentMethod()));
        payment.setStatus(PaymentStatus.CONFIRMED);

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
                payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null,
                payment.getBankName(),
                payment.getAmount(),
                payment.getStatus() != null ? payment.getStatus().name() : null,
                payment.getPaymentDate()
        );
    }

}
