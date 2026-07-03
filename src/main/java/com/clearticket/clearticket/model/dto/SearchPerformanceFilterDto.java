package com.clearticket.clearticket.model.dto;

import com.clearticket.clearticket.model.entity.PerformanceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class SearchPerformanceFilterDto {

    List<String> tagsGenre;
    List<Integer> tagsVibe;
    List<Integer> tagsWith;
    List<PerformanceStatus> statuses;
    List<String> regions;
    LocalDate startDate;
    LocalDate endDate;

    public SearchPerformanceFilterDto() {
        this(null, null, null, null, null, null, null);
    }

    private <T> List<T> getDefaultListIfEmpty(List<T> list, List<T> defaultValue) {
        return (list == null || list.isEmpty()) ? new ArrayList<>(defaultValue) : list;
    }
    public SearchPerformanceFilterDto(List<String> tagsGenre, List<Integer> tagsVibe, List<Integer> tagsWith, List<PerformanceStatus> statuses, List<String> regions, LocalDate startDate, LocalDate endDate) {
        this.tagsGenre = getDefaultListIfEmpty(tagsGenre, List.of("클래식", "뮤지컬", "콘서트"));
        this.tagsVibe = getDefaultListIfEmpty(tagsVibe, List.of(201, 202, 203, 204, 205, 206, 207, 208, 209));
        this.tagsWith = getDefaultListIfEmpty(tagsWith, List.of(301, 302, 303, 304, 305));
        this.statuses = getDefaultListIfEmpty(statuses, List.of(PerformanceStatus.ON_SALE, PerformanceStatus.PREPARING));
        this.regions = getDefaultListIfEmpty(regions, List.of("서울특별시", "인천광역시", "경기도", "강원특별자치도", "대전광역시", "충청북도", "충청남도", "세종특별자치시", "광주광역시", "전북특별자치도", "전라남도", "부산광역시", "대구광역시", "울산광역시", "경상북도", "경상남도", "제주특별자치도", "해외"));
        this.startDate = startDate == null ? LocalDate.now() : startDate;
        this.endDate = endDate == null ? LocalDate.of(LocalDate.now().getYear(), 12, 31) : endDate;
    }
}
