package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.service.OcrService;
import com.clearticket.clearticket.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OcrApiController {

    private final OcrService ocrService;
    private final PerformanceService performanceService;

    @GetMapping("/test-ocr")
    public String testOcr(@RequestParam String url) {
        return ocrService.callOcr(url);
    }

    @GetMapping("/test-db-save")
    public String testDbSave(@RequestParam Long id) {
        return performanceService.testOcrAndSave(id);
    }
    
    @GetMapping("/run-analysis")
    public String runAnalysis(@RequestParam Long id) {
        performanceService.analyzePoster(id); // 서비스의 기능을 호출
        return "DB 저장 완료되었습니다!";
    }
}
