package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.performance.AvailableDateResponse;
import com.clearticket.clearticket.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    /**
     * 포스터 원문(OCR) + AI 3줄 요약을 실행하고 DB(extracted_text, summary_text)에 저장한다.
     * 프론트에서 "AI 요약 생성/재생성" 버튼 클릭 시 호출하면 된다.
     * 예) POST /api/performances/1/analyze-poster
     */
    @PostMapping("/{id}/analyze-poster")
    public ResponseEntity<Void> analyzePoster(@PathVariable Long id) {
        performanceService.analyzePoster(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 디버깅/테스트용: OCR + 3줄 요약 결과를 즉시 문자열로 확인한다.
     * 예) GET /api/performances/1/ocr-test
     */
    @GetMapping("/{id}/ocr-test")
    public ResponseEntity<String> testOcr(@PathVariable Long id) {
        return ResponseEntity.ok(performanceService.testOcrAndSave(id));
    }
}