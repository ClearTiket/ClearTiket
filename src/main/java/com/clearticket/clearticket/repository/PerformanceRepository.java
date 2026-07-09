package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.PerformanceStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    List<Performance> findAllByVenueVenueIdAndStatusIn(Long venueVenueId, Collection<PerformanceStatus> statuses);
    Page<Performance> findAllByRegionIn(Collection<String> regions, Pageable pageable);
    Page<Performance> findAllByGenre(String genre, Pageable pageable);
    List<Performance> findAllByStatusIs(PerformanceStatus status, Limit limit);

    Optional<Performance> findByKopisId(String kopisId);

    // 공연일(종료일)이 지난 공연은 메인/랭킹/지역별 목록에서 제외하기 위한 조회 메서드들
    Page<Performance> findAllByEndDateGreaterThanEqual(LocalDate today, Pageable pageable);
    Page<Performance> findAllByRegionInAndEndDateGreaterThanEqual(Collection<String> regions, LocalDate today, Pageable pageable);
    Page<Performance> findAllByGenreAndEndDateGreaterThanEqual(String genre, LocalDate today, Pageable pageable);
}
