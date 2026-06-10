package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.entity.Reservation;
import com.clearticket.clearticket.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;

    /**
     * 신규 티켓 예매 내역 생성 및 DB저장
     * @param reservation 컨트롤러로 부터 전달받은 예약 정보 데이터
     * @return 최종 저장된 예약 데이터
     */
    public Reservation createReservation(Reservation reservation) {
        if(reservationRepository.existsById(reservation.getReservationId())) {
            throw new IllegalArgumentException("이미 예약된 정보입니다.");
        }
        return reservationRepository.save(reservation);
    }

    /**
     * 예약 고유 ID로 단건 예약 내역 상세 조회
     * @param reservationId 조회하고자 하는 예약의 고유 ID
     * @return 조회에 성공한 예약 엔티티 데이터
     */
    public Reservation getReservationById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 예약 내역이 존재하지 않습니다. ID" + reservationId));
    }
}
