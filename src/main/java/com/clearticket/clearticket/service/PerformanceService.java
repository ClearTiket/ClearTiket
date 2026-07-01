package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.model.dto.performance.ScheduleResponse;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final OcrService ocrService;
    private final AiSummaryService aiSummaryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            // TODO: 지역 텍스트 형식이 Venue 테이블과 Performances가 서로 다름 -> 이거 해결해야됨
            //case "충청" -> regions.addAll(Arrays.asList("충북", "충남", "대전", "세종"));
            //case "전라" -> regions.addAll(Arrays.asList("전북", "전남", "광주"));
            //case "경상" -> regions.addAll(Arrays.asList("경북", "경남", "부산", "대구", "울산"));
            case "서울" -> regions.add("서울특별시");
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
    /**
     * DB의 intro_image_url ( {'styurl': 'http://...'} 형태의 문자열 ) 에서
     * 실제 이미지 URL만 뽑아낸다. styurl이 배열로 오는 경우 첫 번째 값을 사용한다.
     */
    private String resolveIntroImageUrl(Performance p) {
        String raw = p.getIntroImageUrl();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            // DB에 저장된 값이 파이썬 dict 스타일(작은따옴표)이라 큰따옴표로 바꿔서 JSON 파싱
            String normalized = raw.replace("'", "\"");
            JsonNode node = objectMapper.readTree(normalized);
            JsonNode styurl = node.path("styurl");
            if (styurl.isArray()) {
                return styurl.size() > 0 ? styurl.get(0).asText() : null;
            }
            if (!styurl.isMissingNode() && !styurl.isNull()) {
                return styurl.asText();
            }
            return null;
        } catch (Exception e) {
            log.warn("intro_image_url 파싱 실패, 원본 값을 그대로 사용합니다: {}", raw, e);
            return raw; // 파싱 실패 시 원본이 이미 순수 URL일 가능성을 대비해 그대로 반환
        }
    }

    /**
     * OCR 전체 플로우
     * 1) intro_image_url -> 실제 이미지 URL 추출
     * 2) 네이버 CLOVA OCR 호출 -> 원본 텍스트 추출 -> extracted_text 저장
     * 3) OpenAI/Gemini로 3줄 요약 -> summary_text 저장
     */
    @Transactional
    public void analyzePoster(Long performanceId) {
        try {
            Performance p = performanceRepository.findById(performanceId)
                    .orElseThrow(() -> new IllegalArgumentException("공연 없음"));
            log.info(">>> 1. 공연 찾음: {}", p.getTitle());

            String imageUrl = resolveIntroImageUrl(p);
            if (imageUrl == null) {
                log.warn(">>> intro_image_url 이 없어 분석을 건너뜁니다. performanceId={}", performanceId);
                return;
            }

            String rawJson = ocrService.callOcr(imageUrl);
            log.info(">>> 2. 네이버 OCR 응답 완료");

            String extractedText = ocrService.extractTextFromOcr(rawJson);
            log.info(">>> 3. 추출된 텍스트 길이: {}", extractedText != null ? extractedText.length() : 0);
            p.setExtractedText(extractedText);

            String summaryText = aiSummaryService.getSummaryFromText(extractedText);
            log.info(">>> 4. 3줄 요약 완료: {}", summaryText);
            p.setSummaryText(summaryText);

            performanceRepository.save(p);
            log.info(">>> 5. DB 저장 완료 (extracted_text, summary_text)");

        } catch (Exception e) {
            log.error(">>> 에러 발생! performanceId={}", performanceId, e);
        }
    }

    // 테스트/디버깅용: OCR + 요약 결과를 문자열로 바로 확인
    @Transactional
    public String testOcrAndSave(Long performanceId) {
        log.info(">>> 1. 메서드 진입 확인: {}", performanceId);

        Performance p = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음"));

        String imageUrl = resolveIntroImageUrl(p);
        if (imageUrl == null) {
            return "intro_image_url 이 없어 OCR을 수행할 수 없습니다.";
        }

        // 2. OCR 호출
        String rawJson = ocrService.callOcr(imageUrl);
        log.info(">>> 2. OCR 결과: {}", rawJson);

        // 3. 텍스트 추출
        String extractedText = ocrService.extractTextFromOcr(rawJson);
        log.info(">>> 3. 추출된 텍스트: {}", extractedText);
        p.setExtractedText(extractedText);

        // 4. 3줄 요약
        String summaryText = aiSummaryService.getSummaryFromText(extractedText);
        log.info(">>> 4. 요약 결과: {}", summaryText);
        p.setSummaryText(summaryText);

        // 5. DB 저장
        performanceRepository.saveAndFlush(p);
        log.info(">>> 5. DB 저장 완료!");

        return "추출된 텍스트: " + extractedText + "\n\n3줄 요약: " + summaryText;
    }

}