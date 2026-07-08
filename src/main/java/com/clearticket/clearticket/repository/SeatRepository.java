package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * 특정 공연에 설정된 모든 좌석 등급을 중복 없이 조회
     * @param performanceId 조회할 공연의 고유 식별자(ID)
     * @return 공연 내 존재하는 고유한 좌석 등급 리스트 (예: ["VIP석", "R석", "S석"])
     */
    @Query("SELECT DISTINCT s.seatGrade FROM Seat s WHERE s.performance.performanceId = :performanceId")
    List<String> findDistinctByPerformancePerformanceId(@Param("performanceId") Long performanceId);

    List<Seat> findByPerformancePerformanceId(Long performanceId);

    /**
     * 특정 공연의 특정 구역에 존재하는 전체 좌석 수를 조회합니다.
     * 매진 여부 판단 시, 예약 이력이 있는 좌석 수(BookingSeat)만으로는 부족하고
     * 이 값(구역 내 실제 총 좌석 수)과 비교해야 정확한 매진 판정이 가능합니다.
     */
    long countByPerformancePerformanceIdAndSectionName(Long performanceId, String sectionName);
}