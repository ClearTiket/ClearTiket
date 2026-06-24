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
    @GetMapping("/hall/{kopisId}/dates")
    public ResponseEntity<List<AvailableDateResponse>> getAvailableDates(@PathVariable String kopisId) {

        // 1. 서비스단에 2개의 날짜 대신 KOPIS ID 1개만 전달하도록 수정!
        List<AvailableDateResponse> dates = performanceService.calculateAvailableDates(kopisId);

        return ResponseEntity.ok(dates);
    }
}