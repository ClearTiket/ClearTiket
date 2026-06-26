package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.PerformanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    List<Performance> findAllByVenueVenueIdAndStatusIn(Long venueVenueId, Collection<PerformanceStatus> statuses);
    Page<Performance> findAllByRegionIn(Collection<String> regions, Pageable pageable);
    Page<Performance> findAllByGenre(String genre, Pageable pageable);

    Optional<Performance> findByKopisId(String kopisId);
}
