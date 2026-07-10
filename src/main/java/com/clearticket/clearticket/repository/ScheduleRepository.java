package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT DISTINCT s.showDate FROM Schedule s WHERE s.performance.performanceId = :performanceId")
    List<LocalDate> findDistinctDatesByPerformanceId(@Param("performanceId") Long performanceId);

    List<Schedule> findByPerformance_PerformanceIdAndShowDateOrderByRoundNumberAsc(Long performanceId, LocalDate showDate);

    // 실시간 매진 알림 위젯용: 특정 공연의 "가장 가까운 다가오는 회차" 하나를 조회
    java.util.Optional<Schedule> findFirstByPerformance_PerformanceIdAndShowDateGreaterThanEqualOrderByShowDateAscShowTimeAsc(
            Long performanceId, LocalDate today);
}
