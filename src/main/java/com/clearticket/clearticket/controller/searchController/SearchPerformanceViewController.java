package com.clearticket.clearticket.controller.searchController;

import com.clearticket.clearticket.model.document.PerformanceDocument;
import com.clearticket.clearticket.service.searchService.SearchPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchPerformanceViewController {
    private final SearchPerformanceService searchPerformanceService;

    @GetMapping("/result")
    public String searchPerformanceView(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        if (keyword != null && !keyword.trim().isEmpty()) {
            List<PerformanceDocument> searchResults = searchPerformanceService.searchPerformances(keyword);

            model.addAttribute("searchResults", searchResults);
            model.addAttribute("keyword", keyword);
        }

        return "search/result";
    }
}
