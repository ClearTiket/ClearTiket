package com.clearticket.clearticket.model.dto.seat;

public record ScheduleResponse (
    String title,
    String showDate,
    String showTime,
    int roundNumber
) {}
