package com.clearticket.clearticket.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.clearticket.clearticket.model.document.PerformanceDocument;
import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.model.dto.performance.ScheduleResponse;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.PerformanceStatus;
import com.clearticket.clearticket.model.entity.UserTag;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.clearticket.clearticket.repository.UserTagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserTagRepository userTagRepository;
    private final OcrService ocrService;
    private final AiSummaryService aiSummaryService;
    private final ElasticsearchOperations elasticsearchOperations;

    @PersistenceContext
    private EntityManager entityManager;

    int defaultPageSize = 20;

    public Long getPerformanceIdByKopisId(String kopisId) {
        return performanceRepository.findByKopisId(kopisId)
                .map(Performance::getPerformanceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 KOPIS ID와 일치하는 공연이 없습니다: " + kopisId));
    }
    Pageable validPageable(Integer page, Integer pageSize) {
        if (page == null || page <= 0) page = 1;
        page--;
        Pageable pageable = PageRequest.of(page, pageSize);
        return pageable;
    }
    Pageable validPageable(Integer page) {
        return validPageable(page, defaultPageSize);
    }

    public Page<Performance> findAll(Integer page, Integer pageSize) {
        return performanceRepository.findAll(validPageable(page, pageSize));
    }
    public Page<Performance> findAll(int page) {
        return findAll(page, defaultPageSize);
    }

    public List<Performance> findAllByStatus(PerformanceStatus status, int limit) {
        return performanceRepository.findAllByStatusIs(status, Limit.of(limit));
    }

    public List<AvailableDateResponse> calculateAvailableDates(String kopisId) {
        Performance performance = performanceRepository.findByKopisId(kopisId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다: " + kopisId));

        LocalDate startDate = performance.getStartDate();
        LocalDate endDate = performance.getEndDate();

        List<AvailableDateResponse> availableDates = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.toString();
            String dayOfWeek = currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
            availableDates.add(new AvailableDateResponse(dateStr, dayOfWeek, true));
            currentDate = currentDate.plusDays(1);
        }
        return availableDates;
    }

    public List<ScheduleResponse> getSchedulesByDate(Long performanceId, LocalDate date) {
        return scheduleRepository.findByPerformance_PerformanceIdAndShowDateOrderByRoundNumberAsc(performanceId, date)
                .stream()
                .map(schedule -> new ScheduleResponse(
                        schedule.getScheduleId(),
                        schedule.getRoundNumber(),
                        schedule.getShowTime().toString()
                ))
                .collect(Collectors.toList());
    }

    public Page<Performance> findAllByRegion(String region, int page) {

        List<String> regions = new ArrayList<>();
        switch (region) {
            case "충청" -> regions.addAll(Arrays.asList("충청북도", "충청남도", "대전광역시", "세종특별자치시"));
            case "전라" -> regions.addAll(Arrays.asList("전북특별자치도", "전라남도", "광주광역시"));
            case "경상" -> regions.addAll(Arrays.asList("경상북도", "경상남도", "부산광역시", "대구광역시", "울산광역시"));
            default -> regions.add(region);
        }

        return performanceRepository.findAllByRegionIn(regions, validPageable(page));
    }

    public Page<Performance> findRankingAllByGenre(String genre, Integer page) {
        // TODO: 현재는 단순 장르 필터링만 -> 예매율 계산 + 정렬 로직 작성 필요
        return performanceRepository.findAllByGenre(genre, validPageable(page, 10));
    }

    // ========================== OCR 데이터 추출 및 DB 저장=====================================
    // 세로 8000px 이하 포스터 선별
    public boolean isImageValid(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            // 이미지의 헤더 정보만 읽어와서 크기 확인 (전체 다운로드 아님!)
            BufferedImage bimg = ImageIO.read(url);
            if (bimg == null) return false;

            int height = bimg.getHeight();
            System.out.println(">>> 이미지 높이: " + height);

            return height <= 8000;
        } catch (Exception e) {
            return false;
        }
    }
    // JSON 파싱
    private String getSecondImageUrl(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) return null;

        try {
            // 1. 불필요한 문자를 다 제거하고 URL만 찾습니다.
            String url = jsonString.replace("{", "").replace("}", "")
                    .replace("'styurl':", "").replace("'", "")
                    .trim();

            System.out.println(">>> [DEBUG] 추출된 URL 확인: " + url);
            return url;
        } catch (Exception e) {
            return null;
        }
    }
    // 포스터 분석 로직 추가
    @Transactional
    public void analyzePoster(Long performanceId) {
        entityManager.flush(); // 현재까지 바뀐 게 있다면 먼저 반영
        entityManager.clear(); // 1차 캐시 초기화

        Performance p = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음"));

        String targetUrl = getSecondImageUrl(p.getIntroImageUrl());

        // 해상도 검증 로직 적용
        if (targetUrl == null || !isImageValid(targetUrl)) {
            System.out.println(">>> [INFO] 이 포스터는 해상도 제한을 초과하거나 유효하지 않아 건너뜁니다: " + performanceId);
            return; // OCR API 횟수 아끼기 위해 중단
        }

        // 1. OCR 호출
        String rawJson = ocrService.callOcr(targetUrl);

        // 2. 파싱 및 저장
        String extractedText = ocrService.extractTextFromOcr(rawJson);

        // 유효한 텍스트가 있을 때만 저장 (불필요한 update 방지)
        if (extractedText != null && !extractedText.equals("텍스트 추출 실패")) {
            p.setExtractedText(extractedText);

            // 3. GPT 3줄 요약 호출 및 저장
            String summaryText = aiSummaryService.getSummaryFromText(extractedText);
            p.setSummaryText(summaryText);

            performanceRepository.saveAndFlush(p);

            Performance updatedP = performanceRepository.findById(performanceId).orElse(null);
            System.out.println(">>> [DEBUG] DB 재조회 결과 텍스트 길이: " +
                    (updatedP != null && updatedP.getExtractedText() != null ? updatedP.getExtractedText().length() : "NULL"));
            System.out.println(">>> [DEBUG] DB 재조회 결과 요약: " +
                    (updatedP != null ? updatedP.getSummaryText() : "NULL"));
        }
    }
    // 포스터 OCR로 원본 저장
    @Transactional
    public String testOcrAndSave(Long performanceId) {

        Performance p = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음"));

        // 1. intro_image_url은 {'styurl': '...'} 형태의 JSON 문자열이므로 순수 URL만 추출
        String targetUrl = getSecondImageUrl(p.getIntroImageUrl());
        if (targetUrl == null || targetUrl.isBlank()) {
            return "유효한 이미지 URL을 찾을 수 없습니다. intro_image_url 값을 확인하세요.";
        }

        // 2. OCR 호출
        String rawJson = ocrService.callOcr(targetUrl);

        // 3. 텍스트 추출
        String extractedText = ocrService.extractTextFromOcr(rawJson);
        p.setExtractedText(extractedText);

        // 4. GPT 3줄 요약
        String summaryText = aiSummaryService.getSummaryFromText(extractedText);
        p.setSummaryText(summaryText);

        // 5. DB 저장
        performanceRepository.saveAndFlush(p);

        return "추출된 텍스트: " + extractedText + "\n\n3줄 요약: " + summaryText;
    }

    public List<PerformanceDocument> getRecommendedPerformances(Long userId) {
        List<UserTag> tags = userTagRepository.findAllByUserUserId(userId);

        // 취향 태그를 아예 설정하지 않은 회원 → 프론트에서 "취향 설정 안내" 문구를 보여주므로
        // 굳이 검색까지 하지 않고 바로 빈 리스트를 돌려줍니다.
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> userTagsGenre = new ArrayList<>();
        List<Integer> userTagsVibe = new ArrayList<>();
        List<Integer> userTagsWith = new ArrayList<>();

        for (UserTag tag : tags) {
            Integer tagId = tag.getTag().getTagId();
            if (100 <= tagId && tagId < 200) {
                userTagsGenre.add(tag.getTag().getDisplayName());
            } else if (200 <= tagId && tagId < 300) {
                userTagsVibe.add(tagId);
            } else if (300 <= tagId && tagId < 400) {
                userTagsWith.add(tagId);
            }
        }

        List<Query> genreQueries = userTagsGenre.stream()
                .map(id -> Query.of(q -> q.term(t -> t.field("genre").value(id))))
                .collect(Collectors.toList());

        List<Query> vibeQueries = userTagsVibe.stream()
                .map(id -> Query.of(q -> q.term(t -> t.field("tags_vibe").value(id).boost(2.0f))))
                .collect(Collectors.toList());

        List<Query> withQueries = userTagsWith.stream()
                .map(id -> Query.of(q -> q.term(t -> t.field("tags_with").value(id).boost(1.0f))))
                .collect(Collectors.toList());

        // ⚠️ 기존 버그: 장르 태그가 하나도 없으면(genreQueries가 비어있으면) must + minimumShouldMatch("1")
        // 조건이 "0개 중 1개는 반드시 일치해야 함"이 되어 절대 만족될 수 없었고,
        // 그 결과 로그인해서 취향(분위기/동행)만 설정한 회원은 추천 목록이 항상 빈 배열로만 나왔습니다.
        // → 장르 태그가 있을 때만 genre must 절을 추가하도록 수정.
        Query finalQuery = Query.of(q -> q
                .bool(b -> {
                    if (!genreQueries.isEmpty()) {
                        b.must(m -> m
                                .bool(sb -> sb
                                        .should(genreQueries)
                                        .minimumShouldMatch("1")
                                )
                        );
                    }
                    b.should(vibeQueries);
                    b.should(withQueries);
                    return b;
                })
        );

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(finalQuery)
                .withPageable(PageRequest.of(0, 5))
                .build();

        SearchHits<PerformanceDocument> searchHits = elasticsearchOperations.search(nativeQuery, PerformanceDocument.class);

        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}