package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.model.dto.performance.ScheduleResponse;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final OcrService ocrService;
    private final OpenAiService openAiService;

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

        // 🌟 핵심 추가: 해상도 검증 로직 적용
        if (targetUrl == null || !isImageValid(targetUrl)) {
            System.out.println(">>> [INFO] 이 포스터는 해상도 제한을 초과하거나 유효하지 않아 건너뜁니다: " + performanceId);
            return; // OCR API 횟수 아끼기 위해 중단
        }

        // 1. OCR 호출
        String rawJson = ocrService.callOcr(targetUrl);

        // 2. 파싱 및 저장
        String extractedText = ocrService.extractTextFromOcr(rawJson);

        // 🌟 유효한 텍스트가 있을 때만 저장 (불필요한 update 방지)
        if (extractedText != null && !extractedText.equals("텍스트 추출 실패")) {
            p.setExtractedText(extractedText);
            performanceRepository.saveAndFlush(p);
            
            Performance updatedP = performanceRepository.findById(performanceId).orElse(null);
            System.out.println(">>> [DEBUG] DB 재조회 결과 텍스트 길이: " +
                    (updatedP != null && updatedP.getExtractedText() != null ? updatedP.getExtractedText().length() : "NULL"));
        }
    }
    // 포스터 OCR로 원본 저장
    @Transactional
    public String testOcrAndSave(Long performanceId) {

        Performance p = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음"));

        // 2. OCR 호출
        String rawJson = ocrService.callOcr(p.getIntroImageUrl());

        // 3. 텍스트 추출
        String extractedText = ocrService.extractTextFromOcr(rawJson);

        // 4. DB 저장
        p.setExtractedText(extractedText);
        performanceRepository.saveAndFlush(p);

        return "추출된 텍스트: " + extractedText;
    }


}
