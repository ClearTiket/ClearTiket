package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.entity.Performance;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/main")
public class MainPageApiController {

    @GetMapping("/")
    public ResponseEntity<List<Performance>> getMainBannerPerformanceList() {
        List<Performance> performanceList = null;
        return ResponseEntity.ok(performanceList);
    }
}
