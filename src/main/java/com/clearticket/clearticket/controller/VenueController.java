package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.performance.*;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.service.PerformanceService;
import com.clearticket.clearticket.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/hall")
@RequiredArgsConstructor
public class VenueController {

    private final PerformanceRepository performanceRepository;
    private final VenueService VenueService;
    private final PerformanceService performanceService; //

    // 1. 공연 관람 가능 날짜 조회 (KOPIS ID 대신 DB PK인 Long id를 받도록 수정)
    @GetMapping("/{id}/dates")
    public ResponseEntity<List<AvailableDateResponse>> getAvailableDates(
            @PathVariable("id") Long id) {

        // 🌟 KOPIS ID 조회가 아니라 DB의 실제 PK(Long)로 공연을 바로 찾습니다.
        com.clearticket.clearticket.model.entity.Performance performance = performanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 ID입니다: " + id));

        List<AvailableDateResponse> dates = performanceService.calculateAvailableDates(performance.getKopisId());
        return ResponseEntity.ok(dates);
    }

    // 2. 공연 특정 날짜의 회차(세션) 조회 (동일하게 Long id로 처리)
    @GetMapping("/{id}/schedules")
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @PathVariable("id") Long id, // 🌟 Long id로 변경!
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // 🌟 주소창에서 들어온 id가 이미 DB PK(2)이므로 변환기 거칠 필요 없이 바로 찌릅니다!
        List<ScheduleResponse> schedules = performanceService.getSchedulesByDate(id, date);
        return ResponseEntity.ok(schedules);
    }

    // --- 아래 기존 상세 정보, 캐스팅, 리뷰, 레이아웃 API는 그대로 유지합니다 ---
    @GetMapping("/{mt10id}/info")
    public ResponseEntity<VenueInfoResponse> getVenueInfo(@PathVariable("mt10id") String mt10id) {
        VenueInfoResponse venueInfo = VenueService.getVenueInfoByKopisId(mt10id);
        return ResponseEntity.ok(venueInfo);
    }

    @GetMapping("/{mt10id}/casting")
    public ResponseEntity<List<CastingResponse>> getCastingInfo(@PathVariable("mt10id") String mt10id){
        List<CastingResponse> castingList = VenueService.getCastingInfoByKopisId(mt10id);
        return ResponseEntity.ok(castingList);
    }

    @GetMapping("/{mt10id}/reviews")
    public ResponseEntity<ReviewListResponse> getVenueReviews(
            @PathVariable("mt10id") String mt10id,
            @RequestParam(value = "page", defaultValue = "1") int page) {
        ReviewListResponse reviews = VenueService.getReviewsByVenueKopisId(mt10id, page);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{id}/layout")
    public ResponseEntity<VenueLayoutResponse> getVenueLayout(@PathVariable("id") Long id) {
        VenueLayoutResponse layout = VenueService.getVenueLayout(id);
        return ResponseEntity.ok(layout);
    }
}