package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    
    Optional<Performance> findByKopisId(String kopisId);
}
