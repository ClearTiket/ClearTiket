package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
}
