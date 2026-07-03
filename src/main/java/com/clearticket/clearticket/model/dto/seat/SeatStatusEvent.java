package com.clearticket.clearticket.model.dto.seat;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 좌석 상태가 바뀔 때마다 구독자들에게 보내는 브로드캐스트 메시지
@Getter
@AllArgsConstructor
public class SeatStatusEvent {
    private Long seatId;
    private String status; // "BOOKED" or "AVAILABLE"
}