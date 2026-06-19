package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyPageReservationResponseDto {
    private Long reservationId;
    private String reservationNumber;
    private LocalDateTime reservationDate;
    private String performanceTitle;
    private String posterImageUrl;
    private LocalDateTime showDateTime;
    private String venueName;
    private String status;
}