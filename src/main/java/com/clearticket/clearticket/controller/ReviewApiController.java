package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.review.ReviewResponse;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Review;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.service.PerformanceService;
import com.clearticket.clearticket.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;

    public record ReviewWriteRequest(
            Long performanceId,
            Long userId,
            String title,
            String content,
            String type,
            Integer rating
    ) {}

    public record ReviewResultResponse(String result, String message) {}

    @GetMapping
    public ResponseEntity<ReviewResponse> getReviewList(
            @RequestParam("performanceId") Long performanceId,
            @RequestParam(value = "type", defaultValue = "REVIEW") String type,
            @RequestParam(value = "sort", defaultValue = "latest") String sort) {

        Long realPerformanceId = performanceId;

        List<Review> reviews = reviewService.getReviewList(realPerformanceId, type, sort);
        long totalCount = reviewService.getReviewCount(realPerformanceId, type);
        double averageRating = reviewService.getAverageRating(realPerformanceId);

        return ResponseEntity.ok(new ReviewResponse(reviews, totalCount, averageRating));
    }

    @PostMapping("/write")
    public ResponseEntity<ReviewResultResponse> writeReview(@RequestBody ReviewWriteRequest request, HttpSession session ) {

        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(new ReviewResultResponse("FAIL", "로그인이 필요합니다."));
        }

        Long realPerformanceId = request.performanceId();

        Review review = Review.builder()
                .title(request.title())
                .content(request.content())
                .type(request.type())
                .rating(request.rating())
                .performance(Performance.builder().performanceId(realPerformanceId).build())
                .user(User.builder().userId(Long.valueOf(loginUser.getId())).build())
                .build();

        Review savedReview = reviewService.writeReview(review);

        if ("BLIND".equals(savedReview.getStatus())) {
            return ResponseEntity.ok(new ReviewResultResponse("BLINDED", "입력하신 내용에 제한 키워드가 포함되어 블라인드 처리되었습니다."));
        }

        return ResponseEntity.ok(new ReviewResultResponse("SUCCESS", "등록 성공"));
    }

    // [수정] 관람후기 / 기대평 수정 기능 (PUT)
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResultResponse> updateReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestBody ReviewWriteRequest request,
            HttpSession session) {

        // ⚠️ 기존에는 로그인 여부를 전혀 확인하지 않고, 요청 본문(request.userId())에 담긴 값을
        // 그대로 "작성자 본인 확인"에 사용했습니다. 로그인 안 해도 userId만 맞추면 남의 글을
        // 수정할 수 있는 인증 우회(IDOR) 문제였습니다. → 세션 로그인 여부를 먼저 확인합니다.
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(new ReviewResultResponse("FAIL", "로그인이 필요합니다."));
        }
        Long sessionUserId = Long.valueOf(loginUser.getId());

        try {
            Review updatedReview = reviewService.updateReview(reviewId, sessionUserId, request.title(), request.content(), request.rating());

            if ("BLIND".equals(updatedReview.getStatus())) {
                return ResponseEntity.ok(new ReviewResultResponse("BLINDED", "수정하신 내용에 제한 키워드가 포함되어 블라인드 처리되었습니다."));
            }

            return ResponseEntity.ok(new ReviewResultResponse("SUCCESS", "수정 성공"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(new ReviewResultResponse("FAIL", e.getMessage()));
        }
    }

    // [삭제] 관람후기 / 기대평 삭제 (DELETE)
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @PathVariable("reviewId") Long reviewId,
            HttpSession session) {

        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        Long sessionUserId = Long.valueOf(loginUser.getId());

        try {
            reviewService.deleteReview(reviewId, sessionUserId);
            return ResponseEntity.ok("SUCCESS");
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    // [조회수] 더보기 클릭 시 1회 증가 - 세션 중복 방지
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReviewDetail(
            @PathVariable("reviewId") Long reviewId,
            HttpSession session) {

        Set<Long> viewedReviews = (Set<Long>) session.getAttribute("viewedReviews");

        if (viewedReviews == null) {
            viewedReviews = new HashSet<>();
        }

        Review review;

        if (!viewedReviews.contains(reviewId)) {
            review = reviewService.getReviewDetail(reviewId);
            viewedReviews.add(reviewId);
            session.setAttribute("viewedReviews", viewedReviews);
        } else {
            review = reviewService.getReview(reviewId);
        }

        if (review == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "존재하지 않거나 블라인드된 게시글입니다."));
        }

        return ResponseEntity.ok(Map.of(
                "reviewId", review.getReviewId(),
                "views", review.getViews(),
                "content", review.getContent()
        ));
    }
}