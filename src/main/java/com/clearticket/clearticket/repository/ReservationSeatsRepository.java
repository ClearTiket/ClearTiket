package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationSeatsRepository extends JpaRepository<ReservationSeat, Long> {

    // 실시간 매진 알림 위젯용: 특정 회차에 취소되지 않은(WAITING/CONFIRMED) 예매 좌석 수
    @Query("SELECT COUNT(rs) FROM ReservationSeat rs " +
            "WHERE rs.reservation.schedule.scheduleId = :scheduleId " +
            "AND rs.reservation.status <> com.clearticket.clearticket.model.entity.ReservationStatus.CANCELED")
    long countActiveByScheduleId(@Param("scheduleId") Long scheduleId);
}