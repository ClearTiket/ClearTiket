package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.ReservationRequestDto;
import com.clearticket.clearticket.model.dto.ReservationResponseDto;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Reservation;
import com.clearticket.clearticket.model.entity.ReservationStatus;
import com.clearticket.clearticket.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;

    /**
     * 신규 티켓 예매 내역 생성 및 DB저장
     * @param reservationRequestDto 컨트롤러로 부터 전달받은 예약 정보 데이터
     * @return 최종 저장된 예약 데이터
     */
    @Transactional
    public ReservationResponseDto createReservation(
            ReservationRequestDto reservationRequestDto) {

        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.WAITING)
                .build();

        Reservation savedReservation =
                reservationRepository.save(reservation);

        // 주석 : 추후 하드코딩 수정
        return ReservationResponseDto.builder()
                .reservationId(savedReservation.getReservationId())
                .performanceName("BLUE HOUR : 청춘이 빛나는 우리들의 시간") // savedReservation.getPerformance().getTitle()
                .venue("KSPO DOME") // savedReservation.getPerformance().getVenue().getName())
                .totalPrice(158000) // reservationRequestDto.getTotalPrice()
                .status(savedReservation.getStatus().name())
                .build();
    }


    /**
     * 예약 고유 ID로 단건 예약 내역 상세 조회
     * @param reservationId 조회하고자 하는 예약의 고유 ID
     * @return 조회에 성공한 예약 엔티티 데이터
     */
    @Transactional(readOnly = true)
    public ReservationResponseDto getReservationById(Long reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 예약 내역이 존재하지 않습니다. ID: " + reservationId));

        Performance performance = reservation.getPerformance();

        return ReservationResponseDto.builder()
                .reservationId(reservation.getReservationId())
                .performanceName(performance.getTitle())
                .performanceDate(performance.getStartDate().atStartOfDay())
                .venue(String.valueOf(performance.getVenue()))
                .totalPrice(reservation.getTotalPrice())
                .status(reservation.getStatus().name())
                .build();
    }




    /**
     * 전체 예약 내역 목록 조회
     * @return 전체 예약 리스트
     */
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getAllReservations() {

        return reservationRepository.findAll()
                .stream()
                .map(reservation -> ReservationResponseDto.builder()
                        .reservationId(reservation.getReservationId())
                        .totalPrice(reservation.getTotalPrice())
                        .status(reservation.getStatus().name())
                        .build())
                .toList();
    }


    /**
     * 예약 고유 ID로 예매 내역 취소 (상태 변경 방식)
     * @param reservationId 취소하고자 하는 예약 고유 ID
     * @throws IllegalArgumentException 존재하지 않는 예약 ID가 들어왔을 경우 예외 발생
     */
    @Transactional
    public void deleteReservationById(Long reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        reservation.changeStatus(ReservationStatus.CANCELED);
    }
}
