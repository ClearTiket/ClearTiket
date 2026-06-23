package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.review.ReviewResponse; // 레코드 임포트
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Review;
import com.clearticket.clearticket.model.entity.User;
//import com.clearticket.clearticket.service.PerformanceService;
import com.clearticket.clearticket.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewRestController {

    private final ReviewService reviewService;
    //private final PerformanceService performanceService;

    //  1. 관람후기 / 기대평 목록 조회 (Record 적용으로 가독성 극대화)
    public record ReviewWriteRequest(
            String performanceId, //
            Long userId,
            String title,
            String content,
            String type,
            Integer rating
    ) {}

    //  2. 결과 응답용 통일 Record 정의
    public record ReviewResultResponse(String result, String message) {}

    //  [조회] 관람후기 / 기대평 목록 조회 (Record 적용)
    //  프론트엔드가 자바스크립트로 쏠 때 문자열 ID를 던질 수 있으므로 String으로 수신 후 변환
    @GetMapping
    public ResponseEntity<ReviewResponse> getReviewList(
            @RequestParam("performanceId") String performanceKopisId,
            @RequestParam(value = "type", defaultValue = "REVIEW") String type,
            @RequestParam(value = "sort", defaultValue = "latest") String sort) {

        //  변환기: 문자열 KOPIS ID로 팀원 테이블에서 진짜 Long PK를 알아옵니다.
        //Long realPerformanceId = performanceService.getPerformanceIdByKopisId(performanceKopisId);
        Long realPerformanceId = 1L;
        /*if ("PF290842".equals(performanceKopisId)){
            realPerformanceId = 1L;
        }*/

        List<Review> reviews = reviewService.getReviewList(realPerformanceId, type, sort);
        long totalCount = reviewService.getReviewCount(realPerformanceId, type);

        double averageRating = reviewService.getAverageRating(realPerformanceId);

        return ResponseEntity.ok(new ReviewResponse(reviews, totalCount, averageRating));
    }

    // ✍️ [등록] 관람후기 / 기대평 작성 저장 (Record 기반)
    @PostMapping("/write")
    public ResponseEntity<ReviewResultResponse> writeReview(@RequestBody ReviewWriteRequest request) {

        // 변환기: 등록할 때도 문자열 ID를 진짜 숫자 PK로 매핑해 줍니다.
        // [임시] 체크용 =================================================================================================
        //Long realPerformanceId = performanceService.getPerformanceIdByKopisId(request.performanceId());
        Long realPerformanceId = 1L;

        Review review = Review.builder()
                .title(request.title())
                .content(request.content())
                .type(request.type())
                .rating(request.rating())
                .performance(Performance.builder().performanceId(realPerformanceId).build()) // 숫자 PK 주입
                .user(User.builder().userId(request.userId()).build())
                .build();

        Review savedReview = reviewService.writeReview(review);

        if ("BLIND".equals(savedReview.getStatus())) {
            return ResponseEntity.ok(new ReviewResultResponse("BLINDED", "입력하신 내용에 제한 키워드가 포함되어 블라인드 처리되었습니다."));
        }

        return ResponseEntity.ok(new ReviewResultResponse("SUCCESS", "등록 성공"));
    }

    //  [수정] 관람후기 / 기대평 수정 기능 (PUT)
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResultResponse> updateReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestBody ReviewWriteRequest request) {

        // 서비스 단에 수정 데이터를 뭉쳐서 위임
        Review updatedReview = reviewService.updateReview(reviewId, request.userId(), request.title(), request.content(), request.rating());

        if ("BLIND".equals(updatedReview.getStatus())) {
            return ResponseEntity.ok(new ReviewResultResponse("BLINDED", "수정하신 내용에 제한 키워드가 포함되어 블라인드 처리되었습니다."));
        }

        return ResponseEntity.ok(new ReviewResultResponse("SUCCESS", "수정 성공"));
    }

    // [삭제] 관람후기 / 기대평 삭제 (DELETE)
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestParam("userId") Long userId) {

        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok("SUCCESS");
    }
}