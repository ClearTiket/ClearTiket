package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.model.dto.performance.ScheduleResponse;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service // 💡 이 어노테이션이 들어가야 컨트롤러가 일꾼으로 소환할 수 있습니다!
public class PerformanceService {

    public List<AvailableDateResponse> calculateAvailableDates(LocalDate startDate, LocalDate endDate) {
        List<AvailableDateResponse> availableDates = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.toString();
            String dayOfWeek = currentDate.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, Locale.KOREAN);

            availableDates.add(new AvailableDateResponse(dateStr, dayOfWeek, true));
            currentDate = currentDate.plusDays(1);
        }

        return availableDates;
    }
    // 특정 날짜의 회차/시간 조회 로직 (ERD 기반 Mock 데이터 강화)
    public List<ScheduleResponse> getSchedulesByDate(Long performanceId, LocalDate date) {
        List<ScheduleResponse> schedules = new ArrayList<>();

        // 프론트엔드가 달력에서 날짜를 클릭했을 때 '여러 회차'가 나오는 화면을
        // 완벽하게 테스트할 수 있도록 상시 1회차, 2회차 데이터를 제공합니다.
        // 나중에 예비군 다녀오셔서 scheduleRepository.findByPerformanceIdAndShowDate(...) 로 바꾸기 딱 좋은 구조입니다.

        schedules.add(new ScheduleResponse(101L, 1, "14:00")); // schedule_id: 101, round_number: 1, show_time: 14:00
        schedules.add(new ScheduleResponse(102L, 2, "19:30")); // schedule_id: 102, round_number: 2, show_time: 19:30

        return schedules;
    }
}