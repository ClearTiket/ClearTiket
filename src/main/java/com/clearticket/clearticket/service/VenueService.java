package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.PerformanceStatus;
import com.clearticket.clearticket.model.entity.Venue;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VenueService {

    final VenueRepository venueRepository;
    final PerformanceRepository performanceRepository;
    int defaultPageSize = 20;

    Pageable validPageable(Integer page) {
        if (page == null || page <= 0) page = 1;
        page--;
        Pageable pageable = PageRequest.of(page, defaultPageSize);
        return pageable;
    }

    public Venue findById(long id) {
        return venueRepository.findByVenueId(id);
    }

    public Page<Venue> findAll(Integer page) {

        return venueRepository.findAll(validPageable(page));
    }

    public Page<Venue> findByRegion(String region, Integer page) {

        List<String> regions = new ArrayList<>();
        switch (region) {
            case "충청" -> regions.addAll(Arrays.asList("충북", "충남", "대전", "세종"));
            case "전라" -> regions.addAll(Arrays.asList("전북", "전남", "광주"));
            case "경상" -> regions.addAll(Arrays.asList("경북", "경남", "부산", "대구", "울산"));
            default -> regions.add(region);
        }

        return venueRepository.findAllByRegionIn(regions, validPageable(page));
    }

    public List<Performance> findAllByVenueIdAndStatusIn(Long venueId) {
        List<PerformanceStatus> statuses = new ArrayList<>();
        statuses.add(PerformanceStatus.PREPARING);
        statuses.add(PerformanceStatus.ON_SALE);

        List<Performance> canReservePerformances = performanceRepository.findAllByVenueVenueIdAndStatusIn(venueId, statuses);
        return canReservePerformances;
    }
}
