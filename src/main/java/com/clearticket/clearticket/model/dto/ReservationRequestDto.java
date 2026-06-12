package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ReservationRequestDto {
    private Long performanceId;
    private Long userId;
    private List<Long> seatIds;
    private Long couponId;
    private int totalPrice;
    private String ticketType;
}