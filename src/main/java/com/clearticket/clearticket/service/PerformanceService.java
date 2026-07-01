package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.model.dto.performance.ScheduleResponse;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final OcrService ocrService;
    private final OpenAiService openAiService;

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
    // 포스터 분석 로직 추가
    @Transactional
    public void analyzePoster(Long performanceId) {
        try {
            Performance p = performanceRepository.findById(performanceId)
                    .orElseThrow(() -> new IllegalArgumentException("공연 없음"));
            System.out.println(">>> 1. 공연 찾음: " + p.getTitle());

            String rawJson = ocrService.callOcr(p.getPosterUrl());
            System.out.println(">>> 2. 네이버 응답 완료");

            String extractedText = ocrService.extractTextFromOcr(rawJson);
            System.out.println(">>> 3. 추출된 텍스트: " + (extractedText != null ? extractedText.length() : "null"));

            p.setExtractedText(extractedText);
            performanceRepository.save(p);
            System.out.println(">>> 4. 저장 완료");

        } catch (Exception e) {
            System.out.println(">>> 에러 발생! 상세 내용: " + e.getMessage());
            e.printStackTrace(); // 🌟 이 부분이 콘솔에 꼭 찍혀야 합니다!
        }
    }
    // 포스터 OCR로 원본 저장
    @Transactional
    public String testOcrAndSave(Long performanceId) {
        System.out.println(">>> 1. 메서드 진입 확인: " + performanceId); // 로그 1

        Performance p = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음"));

        // 2. OCR 호출
        String rawJson = ocrService.callOcr(p.getPosterUrl());
        System.out.println(">>> 2. OCR 결과: " + rawJson); // 로그 2

        // 3. 텍스트 추출
        String extractedText = ocrService.extractTextFromOcr(rawJson);
        System.out.println(">>> 3. 추출된 텍스트: " + extractedText); // 로그 3

        // 4. DB 저장
        p.setExtractedText(extractedText);
        performanceRepository.saveAndFlush(p);
        System.out.println(">>> 4. DB 저장 완료!"); // 로그 4

        return "추출된 텍스트: " + extractedText;
    }

}
