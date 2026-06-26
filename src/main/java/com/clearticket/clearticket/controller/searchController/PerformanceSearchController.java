package com.clearticket.clearticket.controller.searchController;

import com.clearticket.clearticket.model.document.PerformanceDocument;
import com.clearticket.clearticket.service.searchService.PerformanceSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search/performance")
@RequiredArgsConstructor
public class PerformanceSearchController {
    final PerformanceSearchService performanceSearchService;

    @GetMapping
    public ResponseEntity<List<PerformanceDocument>> performanceSearch(@RequestParam String keyword) {
        return ResponseEntity.ok(performanceSearchService.searchPerformances(keyword));
    }
}
