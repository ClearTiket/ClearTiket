package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyPageWaitingResponseDto {
    private Long waitingId;
    private String performanceTitle;
    private String posterImageUrl;
    private String venueName;
    private LocalDateTime showDateTime;
    private LocalDateTime appliedAt;
    private String seatInfo;
    private int waitingOrder;
    private String status;
}