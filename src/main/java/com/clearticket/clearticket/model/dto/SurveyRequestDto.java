package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SurveyRequestDto {
    private List<String> genres;
    private List<String> moods;
    private String companion;
}