package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SurveyRequestDto {
    /** Q1. 선호 장르 (최대 2개 선택) */
    private List<String> genres;

    /** Q2. 선호 분위기 태그 (최대 2개 선택) */
    private List<String> moods;

    /** Q3. 주 동반자 (1개 선택) */
    private String companion;
}