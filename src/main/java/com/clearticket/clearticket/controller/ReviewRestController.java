package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.review.ReviewResponse; // 레코드 임포트
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Review;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewRestController {

    private final ReviewService reviewService;

    // 🌟 1. 관람후기 / 기대평 목록 조회 (Record 적용으로 가독성 극대화)
    @GetMapping
    public ResponseEntity<ReviewResponse> getReviewList(
            @RequestParam("performanceId") Long performanceId,
            @RequestParam(value = "type", defaultValue = "REVIEW") String type,
            @RequestParam(value = "sort", defaultValue = "latest") String sort) {

        List<Review> list = reviewService.getReviewList(performanceId, type, sort);
        long totalCount = reviewService.getReviewCount(performanceId, type);

        // 💡 HashMap.put() 대신 객체를 껍데기 없이 직관적으로 생성하여 반환!
        return ResponseEntity.ok(new ReviewResponse(list, totalCount));
    }

    // 🌟 2. 관람후기 / 기대평 작성 저장 (POST)
    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> writeReview(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        Long performanceId = Long.valueOf(payload.get("performanceId").toString());
        Long userId = Long.valueOf(payload.get("userId").toString());

        String title = payload.get("title").toString();
        String content = payload.get("content").toString();
        String type = payload.get("type").toString();

        Integer rating = null;
        if (payload.get("rating") != null) {
            rating = Integer.valueOf(payload.get("rating").toString());
        }

        Review review = Review.builder()
                .title(title)
                .content(content)
                .type(type)
                .rating(rating)
                .performance(Performance.builder().performanceId(performanceId).build())
                .user(User.builder().userId(userId).build())
                .build();

        Review savedReview = reviewService.writeReview(review);

        if ("BLIND".equals(savedReview.getStatus())) {
            response.put("result", "BLINDED");
            response.put("message", " 입력하신 내용에 제한 키워드가 포함되어 블라인드 처리되었습니다.");
        } else {
            response.put("result", "SUCCESS");
        }

        return ResponseEntity.ok(response);
    }

    // 🌟 3. 관람후기 / 기대평 삭제 (DELETE)
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestParam("userId") Long userId) {

        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok("SUCCESS");
    }
}