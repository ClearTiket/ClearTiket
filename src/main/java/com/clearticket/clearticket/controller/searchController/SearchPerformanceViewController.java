package com.clearticket.clearticket.controller.searchController;

import com.clearticket.clearticket.model.document.PerformanceDocument;
import com.clearticket.clearticket.service.searchService.SearchPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchPerformanceViewController {
    final SearchPerformanceService searchPerformanceService;

    @GetMapping("/result")
    public String searchPerformanceView() {
        return "search/result";
    }
}
