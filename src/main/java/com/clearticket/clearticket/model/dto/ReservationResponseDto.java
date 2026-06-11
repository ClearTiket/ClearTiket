package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class ReservationResponseDto {
    private Long reservationId;
    private String performanceName;
    private LocalDateTime performanceDate;
    private String venue;
    private List<String> seatInfos;
    private int totalPrice;
    private String status;
}