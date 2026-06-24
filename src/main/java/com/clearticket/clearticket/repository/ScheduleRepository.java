package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByPerformance_PerformanceIdAndShowDateOrderByRoundNumberAsc(Long performanceId, LocalDate showDate);
}
