package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    //  1. 특정 공연의 [관람후기] 또는 [기대평] 목록 최신순 조회 (status = 'Y' 인 것만)
    // 예: 최신순 정렬에 대응
    List<Review> findByPerformance_PerformanceIdAndTypeAndStatusOrderByCreatedAtDesc(Long performanceId, String type, String status);

    //  2. 특정 공연의 [관람후기] 목록 평점순 조회 (status = 'Y' 인 것만)
    // 예: 관람후기 탭에서 '평점순' 정렬을 누를 때 대응
    List<Review> findByPerformance_PerformanceIdAndTypeAndStatusOrderByRatingDescCreatedAtDesc(Long performanceId, String type, String status);

    //  3. 특정 공연에 등록된 특정 타입(후기/기대평)의 총 개수 카운트
    // 와이어프레임의 "총 X개의 관람후기가 등록되었습니다"에 바인딩할 때 쓰입니다.
    long countByPerformance_PerformanceIdAndTypeAndStatus(Long performanceId, String type, String status);

    //  4. 게시글 상세 조회나 수정/삭제 시 status가 'Y'인 정상 글만 가져오기
    Review findByReviewIdAndStatus(Long reviewId, String status);

    //  5. 조회수(views) 증가를 위한 벌크 연산 쿼리
    // 사용자가 아코디언을 열거나 상세 보기 시 카운트를 올릴 때 유용합니다.
    @Modifying
    @Query("UPDATE Review r SET r.views = r.views + 1 WHERE r.reviewId = :reviewId")
    int incrementViews(@Param("reviewId") Long reviewId);

}
