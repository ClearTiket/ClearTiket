package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.entity.Review;
import com.clearticket.clearticket.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;

    // 금칙어 목록을 상수로 명확하게 분리
    private static final String[] FORBIDDEN_WORDS = {"양도", "팝니다", "구매", "판매", "돈받고"};

    //  A. 관람후기 / 기대평 목록 조회
    public List<Review> getReviewList(Long performanceId, String type, String sort) {
        //  버그 수정: 상태 값을 "Y"에서 "ACTIVE"로 매칭 싱크 보정
        if ("rating".equals(sort) && "REVIEW".equals(type)) {
            return reviewRepository.findByPerformance_PerformanceIdAndTypeAndStatusOrderByRatingDescCreatedAtDesc(performanceId, type, "ACTIVE");
        }
        return reviewRepository.findByPerformance_PerformanceIdAndTypeAndStatusOrderByCreatedAtDesc(performanceId, type, "ACTIVE");
    }

    //  B. 총 개수 카운트
    public long getReviewCount(Long performanceId, String type) {
        //  버그 수정: 카운트 역시 "ACTIVE" 상태인 것만 세어야 정상 출력 개수가 맞습니다.
        return reviewRepository.countByPerformance_PerformanceIdAndTypeAndStatus(performanceId, type, "ACTIVE");
    }

    //  C. 글 작성 (금칙어 필터링 포함)
    @Transactional
    public Review writeReview(Review review) {
        // 기본 상태를 "ACTIVE"로 세팅해 두어야 목록 조회에서 정상 출력됩니다.
        review.setStatus("ACTIVE");

        if (review.getContent() != null) {
            for (String word : FORBIDDEN_WORDS) {
                if (review.getContent().contains(word) || (review.getTitle() != null && review.getTitle().contains(word))) {
                    review.setStatus("BLIND");
                    break;
                }
            }
        }

        review.setCreatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    //  D. [새로 추가] 관람후기 / 기대평 수정 기능 (더티 체킹 활용)
    @Transactional
    public Review updateReview(Long reviewId, Long userId, String title, String content, Integer rating) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 방어 코드: 작성자 본인 확인 검증
        if (review.getUser() == null || !review.getUser().getUserId().equals(userId)) {
            throw new SecurityException("수정 권한이 없습니다.");
        }

        // 값 변경 (JPA의 영속성 컨텍스트 덕분에 따로 save를 안 해도 메소드가 끝날 때 자동으로 DB에 반영됩니다)
        review.setTitle(title);
        review.setContent(content);
        review.setUpdatedAt(LocalDateTime.now());

        if (rating != null) {
            review.setRating(rating);
        }

        // 수정된 내용도 금칙어를 다시 한번 검사합니다.
        review.setStatus("ACTIVE"); // 우선 초기화 후 검사
        for (String word : FORBIDDEN_WORDS) {
            if (content.contains(word) || (title != null && title.contains(word))) {
                review.setStatus("BLIND");
                break;
            }
        }

        return review;
    }

    // E. 조회수 증가 및 상세 토글 조회
    @Transactional
    public Review getReviewDetail(Long reviewId) {
        reviewRepository.incrementViews(reviewId);
        //  보정: 상세 조회도 "ACTIVE"인 상태의 글만 가져오도록 싱크 맞춤
        return reviewRepository.findByReviewIdAndStatus(reviewId, "ACTIVE");
    }

    // F. 글 삭제 (소프트 딜리트)
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 방어 코드: 본인 글이 아닐 때 침묵하지 않고 명확하게 예외를 발생시킵니다.
        if (review.getUser() == null || !review.getUser().getUserId().equals(userId)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        review.setStatus("N");
        review.setUpdatedAt(LocalDateTime.now());
    }

    // G. 별점 평균 계산기
    @Transactional
    public double getAverageRating(Long performanceId) {
        return reviewRepository.getAverageRatingByPerformanceId(performanceId).orElse(0.0);
    }
}