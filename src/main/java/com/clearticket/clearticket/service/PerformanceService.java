package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.model.dto.performance.ScheduleResponse;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.ScheduleRepository; // 🌟 주입 완료
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;

    public Long getPerformanceIdByKopisId(String kopisId) {
        return performanceRepository.findByKopisId(kopisId)
                .map(Performance::getPerformanceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 KOPIS ID와 일치하는 공연이 없습니다: " + kopisId));
    }

    public List<AvailableDateResponse> calculateAvailableDates(String kopisId) {
        Performance performance = performanceRepository.findByKopisId(kopisId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다: " + kopisId));

        LocalDate startDate = performance.getStartDate();
        LocalDate endDate = performance.getEndDate();

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
        return scheduleRepository.findByPerformance_PerformanceIdAndShowDateOrderByRoundNumberAsc(performanceId, date)
                .stream()
                .map(schedule -> new ScheduleResponse(
                        schedule.getScheduleId(),
                        schedule.getRoundNumber(),
                        schedule.getShowTime().toString()
                ))
                .collect(Collectors.toList());
    }
}
