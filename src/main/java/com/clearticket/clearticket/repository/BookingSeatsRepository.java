package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BookingSeatsRepository extends JpaRepository<BookingSeat, Long> {

    boolean existsBySeatSeatId(Long seatId);

    // scheduleId 포함 체크 (회차별 선점 분리)
    boolean existsBySeatSeatIdAndScheduleScheduleIdAndCreatedAtAfter(
            Long seatId, Long scheduleId, LocalDateTime dateTime);
}