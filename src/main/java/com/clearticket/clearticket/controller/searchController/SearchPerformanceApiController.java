package com.clearticket.clearticket.controller.searchController;

import com.clearticket.clearticket.model.document.PerformanceDocument;
import com.clearticket.clearticket.model.dto.SearchPerformanceFilterDto;
import com.clearticket.clearticket.service.searchService.AutocorrectSearchService;
import com.clearticket.clearticket.service.searchService.SearchPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/search/performance")
@RequiredArgsConstructor
public class SearchPerformanceApiController {
    final SearchPerformanceService searchPerformanceService;
    private final AutocorrectSearchService autocorrectSearchService;

    @GetMapping("")
    public ResponseEntity<java.util.Map<String, Object>> performanceSearch(
            @RequestParam String keyword) {
        log.info("[통합 조건 검색 가동] 입력 키워드 -> '{}'", keyword);

        List<PerformanceDocument> results = autocorrectSearchService.searchAutocorrect(keyword);

        String correctedKeyword = keyword;

        if (!results.isEmpty()) {
            boolean isExactMatchExist = false;

            for (PerformanceDocument doc : results) {
                String title = doc.getTitle();
                String castings = doc.getCastings();
                String genre = doc.getGenre();

                if ((title != null && title.contains(keyword)) ||
                        (castings != null && castings.contains(keyword)) ||
                        (genre != null && genre.contains(keyword))) {
                    isExactMatchExist = true;
                    break;
                }
            }

            if (!isExactMatchExist) {
                String realTitle = results.get(0).getTitle();
                correctedKeyword = realTitle.replaceAll("\\[.*?\\]", "").trim();
            }
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("correctedKeyword", correctedKeyword);
        response.put("results", results);

        return ResponseEntity.ok(response);
    }

    @PostMapping("")
    public ResponseEntity<List<PerformanceDocument>> performanceSearch(
            @RequestParam String keyword,
            @RequestBody SearchPerformanceFilterDto filterDto) {
        System.out.println(filterDto.getStartDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        return ResponseEntity.ok(searchPerformanceService.searchPerformances(keyword, filterDto));
    }

    @GetMapping("/autocorrect")
    public ResponseEntity<List<PerformanceDocument>> performanceAutocorrectSearch(@RequestParam String keyword) {
        return ResponseEntity.ok(autocorrectSearchService.searchAutocorrect(keyword));
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> getSearchSuggestions(@RequestParam("keyword") String keyword) {
        log.info("[실시간 타이핑 입력 감지]: -> '{}'", keyword);
        List<String> suggestions = autocorrectSearchService.getSuggestions(keyword);
        return ResponseEntity.ok(suggestions);
    }


}
