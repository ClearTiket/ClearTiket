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
}