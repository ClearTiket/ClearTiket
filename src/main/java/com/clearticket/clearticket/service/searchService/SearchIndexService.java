package com.clearticket.clearticket.service.searchService;

import com.clearticket.clearticket.model.document.PerformanceDocument;
import com.clearticket.clearticket.model.document.VenueDocument;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.model.entity.PerformanceTag;
import com.clearticket.clearticket.model.entity.TagCategory;
import com.clearticket.clearticket.model.entity.Venue;
import com.clearticket.clearticket.repository.PerformanceRepository;
import com.clearticket.clearticket.repository.PerformanceTagRepository;
import com.clearticket.clearticket.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 검색(Elasticsearch)에서 "Index venues/performances not found" 에러가 발생하는 문제를 해결하기 위한 서비스.
 * 기존에는 ES 인덱스를 생성/색인하는 코드가 전혀 없어(리포지토리만 정의되어 있고 실제로 save 하는 곳이 없음),
 * ES 서버는 새로 뜨거나 초기화되면 인덱스 자체가 없는 상태였다.
 * 앱 시작 시(SearchIndexInitializer) 인덱스가 없으면 자동으로 생성하고 DB의 공연/공연장 데이터를 색인한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchIndexService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final VenueRepository venueRepository;
    private final PerformanceRepository performanceRepository;
    private final PerformanceTagRepository performanceTagRepository;

    /**
     * venues / performances 인덱스가 존재하지 않으면 생성 후 DB 데이터를 전량 색인한다.
     * 이미 인덱스가 존재하면 아무 작업도 하지 않는다(중복 색인 방지).
     */
    public void reindexAllIfMissing() {
        reindexVenuesIfMissing();
        reindexPerformancesIfMissing();
    }

    public void reindexVenuesIfMissing() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(VenueDocument.class);
        if (indexOps.exists()) {
            return;
        }
        log.warn("[검색색인] venues 인덱스가 존재하지 않아 새로 생성 후 색인을 진행합니다.");
        createIndex(indexOps);
        reindexVenues();
    }

    public void reindexPerformancesIfMissing() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(PerformanceDocument.class);
        if (indexOps.exists()) {
            return;
        }
        log.warn("[검색색인] performances 인덱스가 존재하지 않아 새로 생성 후 색인을 진행합니다.");
        createIndex(indexOps);
        reindexPerformances();
    }

    private void createIndex(IndexOperations indexOps) {
        indexOps.create();
        indexOps.putMapping(indexOps.createMapping());
    }

    /** DB에 있는 모든 공연장을 다시 색인한다. (전체 재색인용, 관리자 기능 등에서 재사용 가능) */
    public void reindexVenues() {
        List<Venue> venues = venueRepository.findAll();
        List<VenueDocument> documents = new ArrayList<>();

        for (Venue venue : venues) {
            VenueDocument doc = new VenueDocument();
            doc.setVenueId(venue.getVenueId());
            doc.setName(venue.getName());
            doc.setAddress(venue.getAddress());
            doc.setRegion(venue.getRegion());
            doc.setTelnum(venue.getTelnum());
            doc.setRelateurl(venue.getRelateurl());
            doc.setCapacity(venue.getCapacity());
            if (venue.getLat() != 0 || venue.getLon() != 0) {
                doc.setLocation(new GeoPoint(venue.getLat(), venue.getLon()));
            }
            documents.add(doc);
        }

        if (!documents.isEmpty()) {
            elasticsearchOperations.save(documents);
        }
        log.info("[검색색인] 공연장 {}건 색인 완료", documents.size());
    }

    /** DB에 있는 모든 공연을 다시 색인한다. */
    public void reindexPerformances() {
        List<Performance> performances = performanceRepository.findAll();

        // 공연별 취향 태그(VIBE/WITH)를 한 번에 조회해서 그룹핑 (N+1 방지)
        Map<Long, List<PerformanceTag>> tagsByPerformanceId = performanceTagRepository.findAll().stream()
                .filter(pt -> pt.getPerformance() != null)
                .collect(Collectors.groupingBy(pt -> pt.getPerformance().getPerformanceId()));

        List<PerformanceDocument> documents = new ArrayList<>();

        for (Performance performance : performances) {
            PerformanceDocument doc = new PerformanceDocument();
            doc.setPerformanceId(performance.getPerformanceId());
            doc.setTitle(performance.getTitle());
            doc.setGenre(performance.getGenre());
            doc.setRegion(performance.getRegion());
            doc.setStatus(performance.getStatus() != null ? performance.getStatus().name() : null);
            doc.setStartDate(performance.getStartDate() != null ? performance.getStartDate().toString() : null);
            doc.setEndDate(performance.getEndDate() != null ? performance.getEndDate().toString() : null);
            doc.setCastings(performance.getCastings());
            doc.setPosterUrl(performance.getPosterUrl());
            doc.setExtractedText(performance.getExtractedText());
            doc.setVenueName(performance.getVenue() != null ? performance.getVenue().getName() : null);

            List<PerformanceTag> tags = tagsByPerformanceId.getOrDefault(performance.getPerformanceId(), List.of());
            doc.setTagsVibe(tags.stream()
                    .filter(pt -> pt.getTag() != null && pt.getTag().getTagCategory() == TagCategory.VIBE)
                    .map(pt -> pt.getTag().getTagId())
                    .collect(Collectors.toList()));
            doc.setTagsWith(tags.stream()
                    .filter(pt -> pt.getTag() != null && pt.getTag().getTagCategory() == TagCategory.WITH)
                    .map(pt -> pt.getTag().getTagId())
                    .collect(Collectors.toList()));

            documents.add(doc);
        }

        if (!documents.isEmpty()) {
            elasticsearchOperations.save(documents);
        }
        log.info("[검색색인] 공연 {}건 색인 완료", documents.size());
    }
}
