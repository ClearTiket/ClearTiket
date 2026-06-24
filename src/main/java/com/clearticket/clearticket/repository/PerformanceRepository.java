package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.PerformanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    List<Performance> findAllByVenueVenueIdAndStatusIn(Long venueVenueId, Collection<PerformanceStatus> statuses);
}
