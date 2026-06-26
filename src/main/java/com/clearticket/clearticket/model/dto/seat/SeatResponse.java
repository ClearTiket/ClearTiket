package com.clearticket.clearticket.model.dto.seat;

import com.clearticket.clearticket.model.entity.Seat;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SeatResponse {
    private Long seatId;
    private String seatGrade;
    private String sectionName;
    private String rowNum;
    private String seatNum;
    private Integer price;
    private String status;

    // 엔티티를 안전하게 DTO 포장지로 변환해 주는 생성자
    public SeatResponse(Seat seat, String status) {
        this.seatId = seat.getSeatId();
        this.seatGrade = seat.getSeatGrade();
        this.sectionName = seat.getSectionName();
        this.rowNum = seat.getRowNum();
        this.seatNum = seat.getSeatNum();
        this.price = seat.getPrice();
        this.status = status;
    }
}