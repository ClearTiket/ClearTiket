package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/performances")
public class PerformanceViewController {

    final PerformanceService performanceService;

    @GetMapping("/region")
    public String regionPerformancesView() {
        return "performances/region";
    }

    @GetMapping("/ranking")
    public String rankingPerformancesView() {
        return "performances/ranking";
    }

    // 💡 공연 가능 날짜 리스트를 달력용으로 쪼개서 반환하는 API
    @GetMapping("/venue/{kopisId}/dates")
    public ResponseEntity<List<AvailableDateResponse>> getAvailableDates(@PathVariable String kopisId) {

        // 1. 서비스단에 2개의 날짜 대신 KOPIS ID 1개만 전달하도록 수정!
        List<AvailableDateResponse> dates = performanceService.calculateAvailableDates(kopisId);

        return ResponseEntity.ok(dates);
    }
}
