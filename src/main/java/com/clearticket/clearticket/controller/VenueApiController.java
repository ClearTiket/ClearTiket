package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.document.VenueDocument;
import com.clearticket.clearticket.model.dto.performance.*;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.service.VenueService;
import com.clearticket.clearticket.service.searchService.SearchVenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/venue")
public class VenueApiController {
    final PerformanceRepository performanceRepository;
    final VenueService venueService;

    @GetMapping("/performances")
    public ResponseEntity<List<Performance>> getCanReservePerformances(@RequestParam Long venueId) {
        List<Performance> performances = venueService.findAllByVenueIdAndStatusIn(venueId);
        return ResponseEntity.ok(performances);
    }

    // 1. 공연 관람 가능 날짜 조회 (KOPIS ID 대신 DB PK인 Long id를 받도록 수정)
    @GetMapping("/{id}/dates")
    public ResponseEntity<List<AvailableDateResponse>> getAvailableDates(@PathVariable("id") Long id) {
        Performance performance = performanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음: " + id));
        return ResponseEntity.ok(venueService.calculateAvailableDates(performance.getKopisId()));
    }

    @GetMapping("/{id}/schedules")
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @PathVariable("id") Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(venueService.getSchedulesByDate(id, date));
    }

    // --- 아래 기존 상세 정보, 캐스팅, 리뷰, 레이아웃 API는 그대로 유지합니다 ---
    @GetMapping("/{id}/info")
    public ResponseEntity<VenueInfoResponse> getVenueInfo(@PathVariable("id") Long id) { // Long 타입으로 변경
        VenueInfoResponse venueInfo = venueService.getVenueInfoByPerformanceId(id);
        return ResponseEntity.ok(venueInfo);
    }

    @GetMapping("/{id}/casting")
    public ResponseEntity<List<CastingResponse>> getCastingInfo(@PathVariable("id") Long id) {
        List<CastingResponse> castingList = venueService.getCastingInfo(id);
        return ResponseEntity.ok(castingList);
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ReviewListResponse> getVenueReviews(
            @PathVariable("id") Long id,
            @RequestParam(value = "page", defaultValue = "1") int page) {
        // performanceId를 인자로 넘겨서 리뷰 조회
        ReviewListResponse reviews = venueService.getReviews(id, page);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{id}/layout")
    public ResponseEntity<VenueLayoutResponse> getVenueLayout(@PathVariable("id") Long id) {
        VenueLayoutResponse layout = venueService.getVenueLayout(id);
        return ResponseEntity.ok(layout);
    }

}
