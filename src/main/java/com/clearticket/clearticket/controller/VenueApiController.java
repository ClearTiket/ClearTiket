package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/venue")
public class VenueApiController {

    final VenueService venueService;

    @GetMapping("/performances")
    public ResponseEntity<List<Performance>> getCanReservePerformances(@RequestParam Long venueId) {
        List<Performance> performances = venueService.findAllByVenueIdAndStatusIn(venueId);
        return ResponseEntity.ok(performances);
    }

}
