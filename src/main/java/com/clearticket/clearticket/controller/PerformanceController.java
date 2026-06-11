package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    // 💡 공연 가능 날짜 리스트를 달력용으로 쪼개서 반환하는 API
    @GetMapping("/dates")
    public ResponseEntity<List<AvailableDateResponse>> getAvailableDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<AvailableDateResponse> dates = performanceService.calculateAvailableDates(startDate, endDate);
        return ResponseEntity.ok(dates);
    }
}