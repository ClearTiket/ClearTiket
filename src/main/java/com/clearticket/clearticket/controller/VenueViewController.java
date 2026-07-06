package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.performance.*;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Schedule;
import com.clearticket.clearticket.model.entity.Venue;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.clearticket.clearticket.service.VenueService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Locale;


@Controller
@RequiredArgsConstructor
@RequestMapping("/venue")
public class VenueViewController {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final VenueService venueService;

    @GetMapping("/{id}/detail") // mt10id 대신 정수형 PK(id) 기반으로 변경
    public String showVenueDetail(@PathVariable("id") Long id, HttpSession session, Model model) {

        // 1. DB에서 실제 2번 공연 정보(공연장 객체 포함) 리얼 타격 조회
        Performance performance = performanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연 ID입니다: " + id));

        // 2. 타임리프 HTML 템플릿에 데이터 통째로 적재
        model.addAttribute("performance", performance);

        // 3. 자바스크립트 달력/회차 fetch 통신용 KOPIS ID도 안전하게 별도 유지
        model.addAttribute("venueKopisId", performance.getKopisId());

        // 4. 자바스크립트 달력/회차
        model.addAttribute("performanceId", performance.getPerformanceId());

        // 5. 세션에서 로그인 사용자 정보
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser != null) {
            model.addAttribute("sessionUserId", loginUser.getId());
        } else {
            model.addAttribute("sessionUserId", null); // 로그인 안 한 경우 null 처리
        }

        // 6. 경로 규격에 맞게 리턴
        return "performances/performance-detail";
    }

    @GetMapping("/seat/selection")
    public String showSeatSelection(@RequestParam Long scheduleId, HttpSession session, Model model) {

        // 좌석 선점/예매는 로그인한 회원만 가능하므로, 세션의 실제 로그인 사용자를 사용합니다.
        // (기존 코드는 JS에서 항상 userId=1로 하드코딩되어 있어 로그인 사용자와 무관하게
        //  DB에 없는 회원으로 예매가 시도되는 버그가 있었습니다.)
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // scheduleId로 실제 회차(Schedule) + 공연(Performance) 정보를 조회해서
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차(scheduleId)입니다: " + scheduleId));

        Performance performance = schedule.getPerformance();

        String formattedDate = schedule.getShowDate()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd(E)", Locale.KOREAN));
        String formattedTime = schedule.getShowTime()
                .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.KOREAN));

        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("performance", performance);
        model.addAttribute("schedule", schedule);
        model.addAttribute("perfTitle", performance.getTitle());
        model.addAttribute("perfDateDisplay",
                formattedDate + " " + formattedTime + " (" + schedule.getRoundNumber() + "회차)");
        model.addAttribute("loginUserId", loginUser.getId());

        return "performances/seat-selection";
    }

    @GetMapping("/list")
    public String venueListView(String region, Integer page, Model model) {

        Page<Venue> venueList;

        if (region == null || region.isEmpty()) {
            venueList = venueService.findAll(page);
        } else {
            venueList = venueService.findByRegion(region, page);
        }

        model.addAttribute("venueList", venueList.getContent());
        model.addAttribute("totalPage", venueList.getTotalPages());
        return "venues/venue-list";
    }

    @GetMapping("/{venueId}")
    public String venueDetailView(@PathVariable Long venueId, Model model) {
        Venue venue = venueService.findById(venueId);
        model.addAttribute("venue", venue);
        return "venues/venue-detail";
    }

}