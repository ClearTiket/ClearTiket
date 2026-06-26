package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.performance.*;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.Review;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.PerformanceStatus;
import com.clearticket.clearticket.model.entity.Venue;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.ReviewRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final ScheduleRepository scheduleRepository;
    private final PerformanceRepository performanceRepository;
    private final ReviewRepository reviewRepository;
    final VenueRepository venueRepository;
    int defaultPageSize = 20;

    public List<AvailableDateResponse> calculateAvailableDates(String kopisId) {
        Performance perf = performanceRepository.findByKopisId(kopisId)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음: " + kopisId));

        List<LocalDate> dates = scheduleRepository.findDistinctDatesByPerformanceId(perf.getPerformanceId());


        return dates.stream()
                .map(date -> new AvailableDateResponse(
                        date.format(java.time.format.DateTimeFormatter.ISO_DATE),
                        date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.KOREAN),
                        true))
                .toList();
    }
    Pageable validPageable(Integer page) {
        if (page == null || page <= 0) page = 1;
        page--;
        Pageable pageable = PageRequest.of(page, defaultPageSize);
        return pageable;
    }

    public List<ScheduleResponse> getSchedulesByDate(Long performanceId, LocalDate date) {
        return scheduleRepository.findByPerformance_PerformanceIdAndShowDateOrderByRoundNumberAsc(performanceId, date)
                .stream()
                .map(s -> new ScheduleResponse(s.getScheduleId(), s.getRoundNumber(), s.getShowTime().format(DateTimeFormatter.ofPattern("HH:mm"))))
                .toList();
    }
    public Venue findById(long id) {
        return venueRepository.findByVenueId(id);
    }

    public Page<Venue> findAll(Integer page) {

        return venueRepository.findAll(validPageable(page));
    }

    public VenueInfoResponse getVenueInfoByPerformanceId(Long id) {
        Performance perf = performanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음"));

        Venue venue = perf.getVenue();

        // Performance 엔티티의 title을 가져와서 DTO에 넣습니다.
        return new VenueInfoResponse(
                perf.getTitle(),
                venue != null ? venue.getName() : "정보 없음",
                300,
                venue != null ? venue.getAddress() : "주소 없음",
                venue != null ? venue.getTelnum() : "000-0000",
                venue != null ? venue.getRelateurl() : "#",
                perf.getPosterUrl(),
                perf.getIntroImageUrl(),
                perf.getStartDate() + " ~ " + perf.getEndDate(),
                perf.getRuntime(),
                perf.getGenre()
        );
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

    public VenueLayoutResponse getVenueLayout(Long venueId) {
        List<SeatGradeInfo> gradeList = Arrays.asList(
                new SeatGradeInfo("VIP", 160000),
                new SeatGradeInfo("R", 140000),
                new SeatGradeInfo("S", 110000)
        );

        // 10행 12열 숫자를 담아 정석대로 반환!
        return new VenueLayoutResponse(venueId, 10, 30, gradeList);
    }


    // 1. 캐스팅 정보 (Performance 엔티티의 castings 문자열 활용)
    public List<CastingResponse> getCastingInfo(Long performanceId) {
        Performance perf = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음"));

        if (perf.getCastings() == null || perf.getCastings().isEmpty()) {
            return List.of();
        }

        // String을 CastingResponse 객체로 변환
        return Arrays.stream(perf.getCastings().split(","))
                .map(String::trim)
                .map(CastingResponse::new)
                .toList();
    }

    // 2. 리뷰 정보 (ReviewRepository 활용)
    public ReviewListResponse getReviews(Long performanceId, int page) {
        // 1. 최신순 정렬
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        // 2. 리포지토리 조회 (Type="REVIEW", Status="Y" 조건 적용)
        List<Review> reviews = reviewRepository.findByPerformance_PerformanceIdAndTypeAndStatus(
                performanceId, "REVIEW", "Y", sort);

        // 3. 평균 평점 계산 (Double 계산)
        Double avgRating = reviewRepository.getAverageRatingByPerformanceId(performanceId).orElse(0.0);

        // 4. DTO 변환 (Review 엔티티 필드명에 맞게 매핑)
        List<ReviewItem> reviewItems = reviews.stream()
                .map(r -> new ReviewItem(
                        r.getReviewId(),
                        r.getUser() != null ? r.getUser().getName() : "익명",
                        r.getContent(),
                        r.getRating(),
                        r.getCreatedAt().toString()
                ))
                .toList();

        return new ReviewListResponse(avgRating, reviews.size(), reviewItems);
    }
    public List<Performance> findAllByVenueIdAndStatusIn(Long venueId) {
        List<PerformanceStatus> statuses = new ArrayList<>();
        statuses.add(PerformanceStatus.PREPARING);
        statuses.add(PerformanceStatus.ON_SALE);

        List<Performance> canReservePerformances = performanceRepository.findAllByVenueVenueIdAndStatusIn(venueId, statuses);
        return canReservePerformances;
    }
}