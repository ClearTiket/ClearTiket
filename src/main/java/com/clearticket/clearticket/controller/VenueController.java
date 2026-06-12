package com.clearticket.clearticket.controller;
import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.model.dto.performance.CastingResponse;
import com.clearticket.clearticket.model.dto.performance.ScheduleResponse;
import com.clearticket.clearticket.model.dto.performance.VenueInfoResponse;
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
public class VenueController { // 변수명과 도메인은 우리 표준인 Venue로 통일

    private final VenueService VenueService;

    // 📅 1. 공연장 관람 가능 날짜 조회
    @GetMapping("/{mt10id}/dates")
    public ResponseEntity<List<AvailableDateResponse>> getAvailableDates(
            @PathVariable("mt10id") String mt10id) { // KOPIS 공연장 ID (예: mt10id)

        List<AvailableDateResponse> dates = VenueService.getDatesByVenueKopisId(mt10id);
        return ResponseEntity.ok(dates);
    }

    // ⏱️ 2. 공연장 특정 날짜의 회차(세션) 조회
    @GetMapping("/{mt10id}/schedules")
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @PathVariable("mt10id") String mt10id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<ScheduleResponse> schedules = VenueService.getSchedulesByVenueAndDate(mt10id, date);
        return ResponseEntity.ok(schedules);
    }

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
}