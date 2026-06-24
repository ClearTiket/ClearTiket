package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.performance.*;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final PerformanceService performanceService;
    private final ScheduleRepository scheduleRepository;
    private final PerformanceRepository performanceRepository;

    // 1. 공연 가능 날짜 조회 (DB 데이터 기반)
    public List<AvailableDateResponse> calculateAvailableDates(String kopisId) {
        Performance perf = performanceRepository.findByKopisId(kopisId)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음"));

        // 실제 DB의 schedule 테이블에서 날짜 목록을 뽑아오거나,
        // performance 테이블의 시작일~종료일 사이의 모든 날짜를 리스트화
        return scheduleRepository.findDistinctDatesByPerformanceId(perf.getPerformanceId());
    }

    // 2. 특정 날짜 회차 조회 (DB 데이터 기반)
    public List<ScheduleResponse> getSchedulesByDate(Long performanceId, LocalDate date) {
        return scheduleRepository.findByPerformanceIdAndDate(performanceId, date)
                .stream()
                .map(s -> new ScheduleResponse(s.getId(), s.getRound(), s.getStartTime()))
                .toList();
    }

    public VenueLayoutResponse getVenueLayout(Long venueId) {
        List<SeatGradeInfo> gradeList = Arrays.asList(
                new SeatGradeInfo("VIP", 150000),
                new SeatGradeInfo("R", 120000),
                new SeatGradeInfo("S", 90000)
        );

        // 10행 12열 숫자를 담아 정석대로 반환!
        return new VenueLayoutResponse(venueId, 10, 12, gradeList);
    }
}