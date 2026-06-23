package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.entity.Venue;
import com.clearticket.clearticket.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/venue")
public class VenueViewController {

    final VenueService venueService;



    @GetMapping("/list")
    public String venueListView(String region, Integer page, Model model) {

        Page<Venue> venueList;

        if (region == null) {
            venueList = venueService.findAll(page);
        } else {
            venueList = venueService.findByRegion(region, page);
        }

        model.addAttribute("venueList", venueList.getContent());
        model.addAttribute("totalPage", venueList.getTotalPages());
        return "venue/venue-list";
    }

    @GetMapping("/{venueId}")
    public String venueDetailView(@PathVariable Long venueId, Model model) {
        Venue venue = venueService.findById(venueId);
        model.addAttribute("venue", venue);
        return "venue/venue-detail";
    }
}
