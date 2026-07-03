package com.clearticket.clearticket.model.dto.seat;

import lombok.*;

// 좌석 상태가 바뀔 때마다 구독자들에게 보내는 브로드캐스트 메시지
@Getter
@AllArgsConstructor
public class SeatStatusEvent {
    private String sectionName;   // A, B, C
    private String rowNum;        // A ~ J
    private Integer seatNum;      // 1 ~ 10
    private String status;        // BOOKED / AVAILABLE
}