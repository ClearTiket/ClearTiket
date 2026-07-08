package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("SELECT DISTINCT s.seatGrade FROM Seat s WHERE s.performance.performanceId = :performanceId")
    List<String> findDistinctByPerformancePerformanceId(@Param("performanceId") Long performanceId);

    List<Seat> findByPerformancePerformanceId(Long performanceId);

    long countByPerformancePerformanceIdAndSectionName(Long performanceId, String sectionName);
}