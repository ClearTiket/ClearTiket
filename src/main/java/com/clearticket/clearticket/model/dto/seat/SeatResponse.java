package com.clearticket.clearticket.model.dto.seat;

import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatResponse {
    private String sectionName;   // A,B,C
    private String rowNum;        // A~J
    private Integer seatNum;      // 1~10
    private String seatGrade;     // VIP,R,S
    private Integer price;        // 가격
    private String status;        // AVAILABLE / BOOKED
    private Long userId;          // 이 좌석을 선점(PENDING)/예매한 회원 ID (내 좌석인지 구분용)
}