package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingSeatsRepository extends JpaRepository<BookingSeat, Long> {

    @Query("SELECT b.seat.seatId FROM BookingSeat b WHERE b.createdAt > :time")
    List<Long> findSeatIdsByCreatedAtAfter(@Param("time") LocalDateTime time);
    boolean existsBySeatSeatIdAndCreatedAtAfter(Long seatId, LocalDateTime dateTime);

    void deleteBySeatSeatIdAndUserUserId(Long seatId, Long userId);

    @Query("SELECT b.seat.seatId FROM BookingSeat b WHERE b.createdAt < :time")
    List<Long> findExpiredSeatIds(@Param("time") LocalDateTime time);

    void deleteByCreatedAtBefore(LocalDateTime time);
}