package com.clearticket.clearticket.model.dto.seat;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatRequest {
    private String sectionName;
    private String rowNum;
    private Integer seatNum;

    private String seatGrade;
    private Integer price;
}
