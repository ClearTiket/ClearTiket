package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class MyPageStatisticsResponseDto {
    private int totalWatchingTime;
    private int totalCount;
    private List<GenreRatioDto> genreRatios;
    private String nextPerformanceDDay;
}