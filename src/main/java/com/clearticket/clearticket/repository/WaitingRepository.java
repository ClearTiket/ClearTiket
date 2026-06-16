package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Schedule;
import com.clearticket.clearticket.model.entity.Venue;
import com.clearticket.clearticket.model.entity.Waiting;
import com.clearticket.clearticket.model.entity.WaitingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WaitingRepository extends JpaRepository<Venue,Long> {

    List<Waiting> findByUserId(Long userId);

    int countByScheduleAndCreatedAtBeforeAndStatus(Schedule schedule, LocalDateTime createdAt, WaitingStatus waitingStatus);
}
