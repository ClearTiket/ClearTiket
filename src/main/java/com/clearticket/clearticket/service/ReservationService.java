package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.ReservationRequestDto;
import com.clearticket.clearticket.model.dto.ReservationResponseDto;
//import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.*;
import com.clearticket.clearticket.repository.ReservationRepository;
import com.clearticket.clearticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;

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


    /**
     * 특정 공연에 설정된 좌석 등급 목록을 중복 없이 조회
     * @param performanceId 조회할 공연의 고유 ID
     * @return 고유한 좌석 등급 리스트
     */
    public List<String> getSeatGrades(Long performanceId) {
        List<String> grades = seatRepository.findDistinctByPerformancePerformanceId(performanceId);

        System.out.println("디버깅: 조회된 등급 리스트 = " + grades);
        return grades;
    }


    /**
     * 예매 상세 페이지에서 사용할 요약 정보(예약 객체, 공연 정보, 좌석 계산 등)를 생성
     * @param reservationId 요약 정보를 생성할 예약 ID
     * @return 화면 렌더링에 필요한 데이터를 담은 Map 객체
     */
    public Map<String, Object> getReservationSummary(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        Performance performance = (Performance) org.hibernate.Hibernate.unproxy(reservation.getSchedule().getPerformance());
        List<ReservationSeat> resSeats = reservation.getReservationSeats();

        System.out.println("디버깅: 조회된 예약 좌석 수 = " + (resSeats != null ? resSeats.size() : "null"));

        int seatPriceSum = 0;
        Set<String> gradeSet = new LinkedHashSet<>();

        for (ReservationSeat rs : resSeats) {
            gradeSet.add(rs.getSeat().getSeatGrade());
            seatPriceSum += rs.getSeat().getPrice();
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("reservation", reservation);
        summary.put("performance", performance);
        summary.put("seatGrade", String.join(", ", gradeSet));
        summary.put("seatCount", resSeats.size());
        summary.put("seatPriceSum", seatPriceSum);

        return summary;
    }
}