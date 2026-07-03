package com.clearticket.clearticket.model.dto;

import com.clearticket.clearticket.model.dto.seat.SeatRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ReservationRequestDto {
    private Long performanceId;
    private Long scheduleId;
    private Long userId;

    private List<SeatRequest> seats;

    private Long couponId;
    private int totalPrice;
    private String ticketType;
}