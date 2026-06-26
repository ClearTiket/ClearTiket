package com.clearticket.clearticket.controller.searchController;

import com.clearticket.clearticket.model.document.PerformanceDocument;
import com.clearticket.clearticket.model.dto.SearchPerformanceFilterDto;
import com.clearticket.clearticket.service.searchService.AutocorrectSearchService;
import com.clearticket.clearticket.service.searchService.SearchPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/search/performance")
@RequiredArgsConstructor
public class SearchPerformanceApiController {
    final SearchPerformanceService searchPerformanceService;
    private final AutocorrectSearchService autocorrectSearchService;

    @GetMapping("")
    public ResponseEntity<List<PerformanceDocument>> performanceSearch(@RequestParam String keyword) {
        return ResponseEntity.ok(searchPerformanceService.searchPerformances(keyword));
    }

    @PostMapping("")
    public ResponseEntity<List<PerformanceDocument>> performanceSearch(@RequestParam String keyword, @RequestBody SearchPerformanceFilterDto filterDto) {
        return null;
    }

    @GetMapping("/autocorrect")
    public ResponseEntity<List<PerformanceDocument>> performanceAutocorrectSearch(@RequestParam String keyword) {
        return ResponseEntity.ok(autocorrectSearchService.searchAutocorrect(keyword));
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> getSearchSuggestions(@RequestParam("keyword") String keyword) {

        log.info("⚡ [실시간 타이핑 입력 감지]: -> '{}'", keyword);
        List<String> suggestions = autocorrectSearchService.getSuggestions(keyword);
        return ResponseEntity.ok(suggestions);
    }


}
