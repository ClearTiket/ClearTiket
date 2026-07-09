package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPageReservationSeatDto {
    private Long resSeatId;
    private String sectionName;
    private String rowNum;
    private Integer seatNum;
    private String seatGrade;
    private Integer price;
}
