package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingSeatsRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByScheduleScheduleId(Long scheduleId);

    boolean existsByScheduleScheduleIdAndSectionNameAndRowNumAndSeatNumAndCreatedAtAfter(
            Long scheduleId,
            String sectionName,
            String rowNum,
            Integer seatNum,
            LocalDateTime dateTime
    );

    void deleteByScheduleScheduleIdAndSectionNameAndRowNumAndSeatNumAndUserUserId(
            Long scheduleId,
            String sectionName,
            String rowNum,
            Integer seatNum,
            Long userId
    );

    List<BookingSeat> findByCreatedAtBefore(LocalDateTime time);

    void deleteByCreatedAtBefore(LocalDateTime time);

    @Query("SELECT bs FROM BookingSeat bs " +
            "WHERE bs.schedule.scheduleId = :scheduleId " +
            "AND bs.sectionName = :sectionName " +
            "AND bs.rowNum = :rowNum " +
            "AND bs.seatNum = :seatNum")
    Optional<BookingSeat> searchByScheduleAndSeat(
            @Param("scheduleId") Long scheduleId,
            @Param("sectionName") String sectionName,
            @Param("rowNum") String rowNum,
            @Param("seatNum") Integer seatNum
    );
}