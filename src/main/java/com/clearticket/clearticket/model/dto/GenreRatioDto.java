package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GenreRatioDto {
    private String genreName;
    private double percentage;
}