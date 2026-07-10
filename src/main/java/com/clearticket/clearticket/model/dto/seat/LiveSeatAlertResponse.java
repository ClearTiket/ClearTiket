package com.clearticket.clearticket.model.dto.seat;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveSeatAlertResponse {
    private Long performanceId;
    private Long scheduleId;
    private String title;        // 공연명
    private String venueName;    // 공연장명
    private String posterUrl;    // 포스터 (썸네일용, 없을 수 있음)
    private int totalSeats;      // 전체 좌석 수
    private int remainingSeats;  // 잔여 좌석 수
}
