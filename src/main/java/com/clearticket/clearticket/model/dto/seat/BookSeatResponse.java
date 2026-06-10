package com.clearticket.clearticket.model.dto.seat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookSeatResponse {
    private Long seatId;
    private Long userId; // 실제 로그인 구현 전까지 사용할 더미 유저 ID
}