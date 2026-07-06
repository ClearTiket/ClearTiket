package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Review;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 1. 특정 공연의 [관람후기/기대평] 목록 조회 (Sort 객체로 최신순/평점순 동적 정렬 처리)
    List<Review> findByPerformance_PerformanceIdAndTypeAndStatus(Long performanceId, String type, String status, Sort sort);

    // 2. 특정 공연에 등록된 [관람후기/기대평] 중 ACTIVE 상태인 총 개수 카운트
    long countByPerformance_PerformanceIdAndTypeAndStatus(Long performanceId, String type, String status);

    // 3. 게시글 상세 조회 시 ACTIVE 상태인 정상 글만 가져오기
    Review findByReviewIdAndStatus(Long reviewId, String status);

    // 4. 조회수(views) 증가를 위한 벌크 연산 쿼리
    @Modifying
    @Query("UPDATE Review r SET r.views = r.views + 1 WHERE r.reviewId = :reviewId")
    int incrementViews(@Param("reviewId") Long reviewId);

    // 5. 특정 공연의 관람후기 평점 평균 계산 (소수점 연산)
    @Query("""
    SELECT AVG(r.rating)
    FROM Review r
    WHERE r.performance.performanceId = :performanceId
    AND r.type = 'REVIEW'
    AND r.status = 'ACTIVE'
    """)
    Optional<Double> getAverageRatingByPerformanceId(@Param("performanceId") Long performanceId);
}