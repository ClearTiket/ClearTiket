package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Reservation;
import com.clearticket.clearticket.model.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * 특정 회원의 예매 내역 중 원하는 상태의 데이터만 필터링 조회
     * @param userId 조회할 회원 ID
     * @param status 찾고자 하는 예매 상태
     * @return 상태 필터링이 완료된 예매 내역 리스트
     */
    List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);

    /**
     * 특정 회원이 구매한 모든 예매 내역 전체 조회
     * @param userId 조회할 회원 ID
     * @return 해당 회원이 가진 전체 예매 내역 리스트
     */
    List<Reservation> findByUserId(Long userId);
}
