package com.clearticket.clearticket.model.dto;

import com.clearticket.clearticket.model.entity.PerformanceStatus;
import lombok.Getter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
public class SearchPerformanceFilterDto {

    // 판매 상태만 프론트에서 실제로 "기본 선택"된 토글(공연중/공연예정)이 있으므로
    // 값이 안 넘어왔을 때(=구버전 클라이언트 등 방어 목적)만 기본값을 채운다.
    private static final List<PerformanceStatus> DEFAULT_STATUS = List.of(PerformanceStatus.ON_SALE, PerformanceStatus.PREPARING);

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

    private <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    // 버그 수정: 리스트가 비어있으면 "기본값으로 채우기"가 아니라 "필터를 아예 걸지 않기"로 처리.
    // 실제 필터 스킵 여부는 SearchPerformanceService에서 리스트가 비어있는지 보고 판단한다.
    public SearchPerformanceFilterDto(List<String> tagsGenre, List<Integer> tagsVibe, List<Integer> tagsWith, List<PerformanceStatus> statuses, List<String> regions, LocalDate startDate, LocalDate endDate) {
        this.tagsGenre = emptyIfNull(tagsGenre);
        this.tagsVibe = emptyIfNull(tagsVibe);
        this.tagsWith = emptyIfNull(tagsWith);
        this.statuses = (statuses == null || statuses.isEmpty()) ? new ArrayList<>(DEFAULT_STATUS) : statuses;
        this.regions = emptyIfNull(regions);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void setTagsGenre(List<String> tagsGenre) { this.tagsGenre = emptyIfNull(tagsGenre); }
    public void setTagsVibe(List<Integer> tagsVibe) { this.tagsVibe = emptyIfNull(tagsVibe); }
    public void setTagsWith(List<Integer> tagsWith) { this.tagsWith = emptyIfNull(tagsWith); }
    public void setStatuses(List<PerformanceStatus> statuses) { this.statuses = (statuses == null || statuses.isEmpty()) ? new ArrayList<>(DEFAULT_STATUS) : statuses; }
    public void setRegions(List<String> regions) { this.regions = emptyIfNull(regions); }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}