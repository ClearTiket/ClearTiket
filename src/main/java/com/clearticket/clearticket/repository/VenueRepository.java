package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    Venue findByVenueId(long id);
    //List<Venue> findAllByRegion(String region, Pageable pageable);
    Page<Venue> findAllByRegion(String region, Pageable pageable);
    Page<Venue> findAllByRegionIn(Collection<String> regions, Pageable pageable);
}
