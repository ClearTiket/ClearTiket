package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.repository.PerformanceRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class VenueViewController {

    private final PerformanceRepository performanceRepository;

    @GetMapping("/hall/{id}/detail") // mt10id 대신 정수형 PK(id) 기반으로 변경
    public String showVenueDetail(@PathVariable("id") Long id, Model model) {

        // 1. DB에서 실제 2번 공연 정보(공연장 객체 포함) 리얼 타격 조회
        Performance performance = performanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 ID입니다: " + id));

        // 2. 타임리프 HTML 템플릿에 데이터 통째로 적재
        model.addAttribute("performance", performance);

        // 3. 자바스크립트 달력/회차 fetch 통신용 KOPIS ID도 안전하게 별도 유지
        model.addAttribute("venueKopisId", performance.getKopisId());

        // 4. 경로 규격에 맞게 리턴
        return "performances/performance-detail";
    }

    @GetMapping("/seat/selection")
    public String showSeatSelection() {
        return "performances/seat-selection";
    }
}