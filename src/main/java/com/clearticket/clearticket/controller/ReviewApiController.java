package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.review.ReviewResponse;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Review;
import com.clearticket.clearticket.model.entity.User;
//import com.clearticket.clearticket.service.PerformanceService; // 팀원 서비스 임포트 복구
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

    // 1. 관람후기 / 기대평 목록 조회 (Record 적용으로 가독성 극대화)
    public record ReviewWriteRequest(
            Long performanceId,
            Long userId,
            String title,
            String content,
            String type,
            Integer rating
    ) {}

    // 2. 결과 응답용 통일 Record 정의
    public record ReviewResultResponse(String result, String message) {}

    // [조회] 관람후기 / 기대평 목록 조회 (Record 적용)
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

    //  [등록] 관람후기 / 기대평 작성 저장
    @PostMapping("/write")
    public ResponseEntity<ReviewResultResponse> writeReview(@RequestBody ReviewWriteRequest request, HttpSession session ) {

        // 세션에서 현재 로그인한 유저를 가져옵니다.
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(new ReviewResultResponse("FAIL", "로그인이 필요합니다."));
        }

        // 변환기: 등록할 때도 문자열 ID를 진짜 숫자 PK로 매핑해 줍니다.
        Long realPerformanceId = request.performanceId();

        Review review = Review.builder()
                .title(request.title())
                .content(request.content())
                .type(request.type())
                .rating(request.rating())
                .performance(Performance.builder().performanceId(realPerformanceId).build()) // 숫자 PK 주입
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
        // 그대로 "작성자 본인 확인"에 사용했습니다. 즉 로그인하지 않은 사용자도 본문에
        // 작성자 userId만 알아내서 넣으면 수정이 가능한 심각한 인증 우회(IDOR) 문제였습니다.
        // → 반드시 세션에 로그인된 사용자인지 확인하고, 그 세션의 userId만 신뢰합니다.
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

        // ⚠️ 위와 동일한 이유로, 쿼리파라미터로 넘어오는 userId를 더 이상 신뢰하지 않고
        // 세션에 실제로 로그인된 사용자인지 먼저 확인합니다.
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

    // [수정] 조회수(더보기 클릭 시 1회 증가 - 세션 중복 방지 탑재)
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReviewDetail(
            @PathVariable("reviewId") Long reviewId,
            HttpSession session) {

        // 1. 세션에서 이 유저가 읽은 후기 ID 목록(Set)을 꺼내옵니다.
        Set<Long> viewedReviews = (Set<Long>) session.getAttribute("viewedReviews");

        // 2. 만약 세션에 목록이 처음이라 비어있다면 가방을 새로 생성해 줍니다.
        if (viewedReviews == null) {
            viewedReviews = new HashSet<>();
        }

        Review review;

        // 3. 가방 안에 현재 후기 ID가 없다면? 처음 읽는 글이므로 조회수를 올립니다.
        if (!viewedReviews.contains(reviewId)) {
            review = reviewService.getReviewDetail(reviewId); // 서비스 내의 incrementViews 작동

            // 4. 읽은 목록에 새롭게 추가하고 세션을 동기화합니다.
            viewedReviews.add(reviewId);
            session.setAttribute("viewedReviews", viewedReviews);
        } else {
            // 5. 이미 가방에 ID가 들어있다면? 조회수 증가 없이 ACTIVE 상태인 데이터만 안전하게 필터링해 반환합니다.
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