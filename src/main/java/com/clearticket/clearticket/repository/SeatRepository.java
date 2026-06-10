package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByPerformancePerformanceId(Long performanceId);
}