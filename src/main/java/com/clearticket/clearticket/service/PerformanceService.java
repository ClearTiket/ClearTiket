package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.model.dto.performance.ScheduleResponse;
import com.clearticket.clearticket.model.entity.Performance;
//import com.clearticket.clearticket.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PerformanceService {

//    private final PerformanceRepository performanceRepository;

//    // [추가] 컨트롤러에서 호출할 "문자열 ID ➔ 숫자 PK" 핵심 변환기 메서드
//    public Long getPerformanceIdByKopisId(String kopisId) {
//        return performanceRepository.findByKopisId(kopisId)
//                .map(Performance::getPerformanceId) // 엔티티 내의 PK 필드명이 performanceId인 경우
//                .orElseThrow(() -> new IllegalArgumentException("해당 KOPIS ID와 일치하는 공연이 없습니다: " + kopisId));
//    }

    public List<AvailableDateResponse> calculateAvailableDates(LocalDate startDate, LocalDate endDate) {
        List<AvailableDateResponse> availableDates = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.toString();
            String dayOfWeek = currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
            availableDates.add(new AvailableDateResponse(dateStr, dayOfWeek, true));
            currentDate = currentDate.plusDays(1);
        }
        return availableDates;
    }

    public List<ScheduleResponse> getSchedulesByDate(Long performanceId, LocalDate date) {
        List<ScheduleResponse> schedules = new ArrayList<>();
        schedules.add(new ScheduleResponse(101L, 1, "14:00"));
        schedules.add(new ScheduleResponse(102L, 2, "19:30"));
        return schedules;
    }
}