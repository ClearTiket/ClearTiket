package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.ReservationRequestDto;
import com.clearticket.clearticket.model.dto.ReservationResponseDto;
//import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Reservation;
import com.clearticket.clearticket.model.entity.ReservationStatus;
import com.clearticket.clearticket.model.entity.Schedule;
import com.clearticket.clearticket.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public ReservationResponseDto createReservation(ReservationRequestDto reservationRequestDto) {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.WAITING);
        reservation.setTotalPrice(reservationRequestDto.getTotalPrice());
        reservation.setTicketType(reservationRequestDto.getTicketType());
        reservation.setShippingFee(0);
        reservation.setReservationNumber("RSV-" + System.currentTimeMillis());

        Reservation savedReservation = reservationRepository.save(reservation);

        return toResponseDto(savedReservation);
    }


    /**
     * 예약 고유 ID로 단건 예약 내역 상세 조회
     * @param reservationId 조회하고자 하는 예약의 고유 ID
     * @return 조회에 성공한 예약 엔티티 데이터
     */
    @Transactional(readOnly = true)
    public ReservationResponseDto getReservationById(Long reservationId) {
        Optional<Reservation> optionalReservation = reservationRepository.findById(reservationId);

        if (optionalReservation.isEmpty()) {
            throw new IllegalArgumentException("해당 예약 내역이 존재하지 않습니다. ID: " + reservationId);
        }

        Reservation reservation = optionalReservation.get();

        return toResponseDto(reservation);
    }


    /**
     * 전체 예약 내역 목록 조회
     * @return 전체 예약 리스트
     */
    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getAllReservations() {
        List<Reservation> reservations = reservationRepository.findAll();
        List<ReservationResponseDto> result = new ArrayList<>();

        for (Reservation reservation : reservations) {
            result.add(toResponseDto(reservation));
        }

        return result;
    }


    /**
     * 예약 고유 ID로 예매 내역 취소 (상태 변경 방식)
     * @param reservationId 취소하고자 하는 예약 고유 ID
     * @throws IllegalArgumentException 존재하지 않는 예약 ID가 들어왔을 경우 예외 발생
     */
    @Transactional
    public void cancelReservationById(Long reservationId) {

        Optional<Reservation> optionalReservation = reservationRepository.findById(reservationId);

        if (optionalReservation.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 예약입니다. ID: " + reservationId);
        }

        Reservation reservation = optionalReservation.get();
        reservation.changeStatus(ReservationStatus.CANCELED);
    }


    /**
     * 예약 엔티티 데이터를 응답 DTO 객체로 변환
     * @param reservation 변환할 예약 엔티티 원본 데이터
     * @return 예약 정보 및 연관 공연 정보가 포함된 최종 응답 DTO 객체
     */
    private ReservationResponseDto toResponseDto(Reservation reservation) {
        Schedule schedule = reservation.getSchedule();
        Performance performance = (schedule != null) ? schedule.getPerformance() : null;

        LocalDateTime perfDateTime = null;
        if (schedule != null && schedule.getShowDate() != null && schedule.getShowTime() != null) {
            perfDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
        }

        return new ReservationResponseDto(
                reservation.getReservationId(),
                performance != null ? performance.getTitle() : "공연 정보 없음",
                perfDateTime,
                performance != null && performance.getVenue() != null ? performance.getVenue().getName() : "장소 정보 없음",
                null,
                reservation.getTotalPrice(),
                reservation.getStatus() != null ? reservation.getStatus().name() : null
        );
    }


}