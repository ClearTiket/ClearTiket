package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.seat.ScheduleResponse;
import com.clearticket.clearticket.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedules") // 기본 경로 설정
@RequiredArgsConstructor
public class ScheduleApiController {

    private final ScheduleRepository scheduleRepository;

    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getScheduleInfo(@PathVariable Long scheduleId) {
        var schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄을 찾을 수 없습니다."));
        
        return ResponseEntity.ok(new ScheduleResponse(
                schedule.getPerformance().getTitle(),
                schedule.getShowDate().toString(),
                schedule.getShowTime().toString(),
                schedule.getRoundNumber()
        ));
    }
}
