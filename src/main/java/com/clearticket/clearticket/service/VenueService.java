package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.entity.Venue;
import com.clearticket.clearticket.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {

    final VenueRepository venueRepository;

    public Venue findById(long id) {
        return venueRepository.findByVenueId(id);
    }

    public List<Venue> findAll(Pageable pageable) {

        Page<Venue> result = venueRepository.findAll(pageable);

        return result.getContent();
    }

    public List<Venue> findByRegion(String region, Pageable pageable) {
        return venueRepository.findAllByRegion(region, pageable);
    }
}
