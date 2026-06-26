package com.clearticket.clearticket.model.dto.performance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AvailableDateResponse {
    private String date;       // "2026-06-11" 형태의 날짜 문자열
    private String dayOfWeek;  // "목요일" 같은 요일 정보 (달력 표시용)
    private boolean isAvailable; // 예매 가능 여부 (무조건 true로 채워서 보냄)
}