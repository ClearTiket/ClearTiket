package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SearchPerformanceFilterDto {
    List<String> tagsGenre;
    List<String> tagsVibe;
    List<String> tagsWith;
    List<String> statuses;
    List<String> regions;
}
