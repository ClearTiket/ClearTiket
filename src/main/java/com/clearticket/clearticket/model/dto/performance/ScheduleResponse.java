package com.clearticket.clearticket.model.dto.performance;
import lombok.*;

@Getter
@AllArgsConstructor
public class ScheduleResponse {
    private Long scheduleId;    // 공연 날짜 확인
    private int round;          // 공연 회차
    private String startTime;   // 공연 시작 시간
}
