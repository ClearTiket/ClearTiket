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

    // 🌟 A. 관람후기 / 기대평 목록 조회
    public List<Review> getReviewList(Long performanceId, String type, String sort) {
        if ("rating".equals(sort) && "REVIEW".equals(type)) {
            // 관람후기 탭이면서 평점순 정렬을 원할 때
            return reviewRepository.findByPerformance_PerformanceIdAndTypeAndStatusOrderByRatingDescCreatedAtDesc(performanceId, type, "Y");
        }
        // 기본값: 최신순 정렬
        return reviewRepository.findByPerformance_PerformanceIdAndTypeAndStatusOrderByCreatedAtDesc(performanceId, type, "Y");
    }

    // 🌟 B. 총 개수 카운트
    public long getReviewCount(Long performanceId, String type) {
        return reviewRepository.countByPerformance_PerformanceIdAndTypeAndStatus(performanceId, type, "Y");
    }

    // 🌟 C. 글 작성 (금칙어 필터링 포함)
    @Transactional
    public Review writeReview(Review review) {
        // 간단한 금칙어 목록 정의 (티켓 양도 거래 차단용)
        String[] forbiddenWords = {"양도", "팝니다", "구매", "판매", "돈받고"};

        if (review.getContent() != null) {
            for (String word : forbiddenWords) {
                if (review.getContent().contains(word) || (review.getTitle() != null && review.getTitle().contains(word))) {
                    review.setStatus("BLIND"); // 🔒 하나라도 걸리면 자동으로 블라인드 상태로 저장!
                    break;
                }
            }
        }

        review.setCreatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    // 🌟 D. 조회수 증가 및 상세 토글 조회
    @Transactional
    public Review getReviewDetail(Long reviewId) {
        reviewRepository.incrementViews(reviewId); // 조회수 +1
        return reviewRepository.findByReviewIdAndStatus(reviewId, "Y");
    }

    // 🌟 E. 글 삭제 (소프트 딜리트)
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 작성자 본인 확인 검증 로직 추가 가능
        if (review.getUser() != null && review.getUser().getUserId().equals(userId)) {
            review.setStatus("N");
            review.setUpdatedAt(LocalDateTime.now());
        }
    }
}