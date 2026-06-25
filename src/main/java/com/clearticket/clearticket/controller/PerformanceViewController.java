package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/performances")
public class PerformanceViewController {

    final PerformanceService performanceService;

    @GetMapping("/region")
    public String regionPerformancesView() {
        return "performances/region";
    }

    @GetMapping("/ranking")
    public String rankingPerformancesView() {
        return "performances/ranking";
    }
}
