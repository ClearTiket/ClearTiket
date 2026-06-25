package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Schedule;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@Controller
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatViewController {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;

    /**
     * 좌석 선택 페이지
     * URL: /seats?performanceId=1&scheduleId=501
     */
    @GetMapping
    public String seatSelectionPage(
            @RequestParam("performanceId") Long performanceId,
            @RequestParam("scheduleId") Long scheduleId,
            HttpSession session,
            Model model) {

        // 1. 로그인 체크
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        // 2. 공연 정보 조회
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다."));

        // 3. 회차(스케줄) 정보 조회
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        // 4. 날짜/시간 포맷 가공 (예: "2026.06.24(수) 07:30 PM (1회차)")
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
        String dayOfWeek = schedule.getShowDate()
                .getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        String scheduleLabel = String.format("%s(%s) %s (%d회차)",
                schedule.getShowDate().format(dateFmt),
                dayOfWeek,
                schedule.getShowTime().format(timeFmt),
                schedule.getRoundNumber());

        // 5. 모델에 담기
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("performanceId", performanceId);
        model.addAttribute("scheduleId", scheduleId);
        model.addAttribute("performanceTitle", performance.getTitle());
        model.addAttribute("scheduleLabel", scheduleLabel);

        return "performances/seat-selection";
    }
}