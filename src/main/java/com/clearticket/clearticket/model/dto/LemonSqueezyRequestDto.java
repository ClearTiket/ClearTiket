package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LemonSqueezyRequestDto {
    private Long reservationId;     // 연관된 예약 고유 ID
    private Long amount;            // 결제할 총 금액
    private String performanceTitle; // 가상 결제창에 띄워줄 공연 제목
}