package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/performances")
public class PerformanceApiController {

    final PerformanceService performanceService;

    @GetMapping("/region")
    public ResponseEntity<List<Performance>> getPerformancesByRegion (@RequestParam String region, @RequestParam int page) {

        Page<Performance> performances;
        if (region == null || region.isEmpty()) {
            performances = performanceService.findAll(page);
        } else {
            performances = performanceService.findAllByRegion(region, page);
        }

        return ResponseEntity.ok(performances.getContent());
    }
}
