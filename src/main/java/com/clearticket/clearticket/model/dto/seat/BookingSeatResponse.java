package com.clearticket.clearticket.model.dto.seat;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatResponse {
    private Long seatId;
    private Long userId;

    private String sectionName;
    private String rowNum;
    private Integer seatNum;
}