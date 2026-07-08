package com.clearticket.clearticket.model.dto;

import com.clearticket.clearticket.model.entity.PerformanceStatus;
import lombok.Getter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
public class SearchPerformanceFilterDto {

    private static final List<String> DEFAULT_GENRE = List.of("클래식", "뮤지컬", "콘서트");
    private static final List<Integer> DEFAULT_VIBE = List.of(201, 202, 203, 204, 205, 206, 207, 208, 209);
    private static final List<Integer> DEFAULT_WITH = List.of(301, 302, 303, 304, 305);
    private static final List<PerformanceStatus> DEFAULT_STATUS = List.of(PerformanceStatus.ON_SALE, PerformanceStatus.PREPARING);
    private static final List<String> DEFAULT_REGION = List.of("서울특별시", "인천광역시", "경기도", "강원특별자치도", "대전광역시", "충청북도", "충청남도", "세종특별자치시", "광주광역시", "전북특별자치도", "전라남도", "부산광역시", "대구광역시", "울산광역시", "경상북도", "경상남도", "제주특별자치도", "해외");

    private List<String> tagsGenre;
    private List<Integer> tagsVibe;
    private List<Integer> tagsWith;
    private List<PerformanceStatus> statuses;
    private List<String> regions;
    private LocalDate startDate;
    private LocalDate endDate;

    public SearchPerformanceFilterDto() {
        this(null, null, null, null, null, null, null);
    }

    private <T> List<T> getDefaultListIfEmpty(List<T> list, List<T> defaultValue) {
        return (list == null || list.isEmpty()) ? new ArrayList<>(defaultValue) : list;
    }

    public SearchPerformanceFilterDto(List<String> tagsGenre, List<Integer> tagsVibe, List<Integer> tagsWith, List<PerformanceStatus> statuses, List<String> regions, LocalDate startDate, LocalDate endDate) {
        this.tagsGenre = getDefaultListIfEmpty(tagsGenre, DEFAULT_GENRE);
        this.tagsVibe = getDefaultListIfEmpty(tagsVibe, DEFAULT_VIBE);
        this.tagsWith = getDefaultListIfEmpty(tagsWith, DEFAULT_WITH);
        this.statuses = getDefaultListIfEmpty(statuses, DEFAULT_STATUS);
        this.regions = getDefaultListIfEmpty(regions, DEFAULT_REGION);
        this.startDate = startDate == null ? LocalDate.now() : startDate;
        this.endDate = endDate == null ? LocalDate.of(LocalDate.now().getYear(), 12, 31) : endDate;
    }

    // Jackson이 이 setter들을 통해 값을 채우되, 빈 배열이면 그대로 두지 않고 기본값으로 치환
    public void setTagsGenre(List<String> tagsGenre) { this.tagsGenre = getDefaultListIfEmpty(tagsGenre, DEFAULT_GENRE); }
    public void setTagsVibe(List<Integer> tagsVibe) { this.tagsVibe = getDefaultListIfEmpty(tagsVibe, DEFAULT_VIBE); }
    public void setTagsWith(List<Integer> tagsWith) { this.tagsWith = getDefaultListIfEmpty(tagsWith, DEFAULT_WITH); }
    public void setStatuses(List<PerformanceStatus> statuses) { this.statuses = getDefaultListIfEmpty(statuses, DEFAULT_STATUS); }
    public void setRegions(List<String> regions) { this.regions = getDefaultListIfEmpty(regions, DEFAULT_REGION); }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate == null ? LocalDate.now() : startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate == null ? LocalDate.of(LocalDate.now().getYear(), 12, 31) : endDate; }
}