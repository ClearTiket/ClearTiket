package com.clearticket.clearticket.service.searchService;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import com.clearticket.clearticket.model.document.PerformanceDocument;
import com.clearticket.clearticket.model.document.VenueDocument;
import com.clearticket.clearticket.model.vo.VenueSearchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchVenueService {

    final ElasticsearchOperations elasticsearchOperations;

    public VenueSearchResultVO searchVenues(String searchText, String region, Integer page) {

        if (page == null || page < 1) page = 1;
        page--;

        // 공백 기준 검색어를 키워드로 분리
        List<String> keywords = Arrays.stream(searchText.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .toList();

        // 검색어가 없거나 공백만 존재하는 경우 빈 List 반환
        if (keywords.isEmpty()) return new VenueSearchResultVO(new ArrayList<>(), 0);

        List<Query> mustQueries = new ArrayList<>();   // 모두 만족해야 하는 조건들

        // 분리된 검색어를 must query에 입력
        for (String keyword : keywords) {
            mustQueries.add(Query.of(
                    q -> q.match(m -> m.field("name").query(keyword))));
        }


        if (region != null && !region.isEmpty()) {
            List<String> regions = new ArrayList<>();
            switch (region) {
                case "충청" -> regions.addAll(Arrays.asList("충북", "충남", "대전", "세종"));
                case "전라" -> regions.addAll(Arrays.asList("전북", "전남", "광주"));
                case "경상" -> regions.addAll(Arrays.asList("경북", "경남", "부산", "울산", "대구"));
                default -> regions.add(region);
            }
            mustQueries.add(Query.of(q -> q.terms(
                    t -> t
                            .field("region")
                            .terms(TermsQueryField.of(v -> v.value(regions
                                    .stream()
                                    .map(FieldValue::of)
                                    .collect(Collectors.toList()))))
            )));
        }

        // 최종 bool 쿼리 생성
        Query finalBoolQuery = Query.of(
                q -> q.bool(b -> b.must(mustQueries)));

        // native query를 통해 전송 규격으로 래핑
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(finalBoolQuery)
                .withPageable(PageRequest.of(page, 20)) // 검색 결과 점수 상위 20개
                .build();

        SearchHits<VenueDocument> searchHits = elasticsearchOperations.search(nativeQuery, VenueDocument.class);
        SearchPage<VenueDocument> searchPage = SearchHitSupport.searchPageFor(searchHits, nativeQuery.getPageable());

        var result = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // System.out.println(searchPage.getTotalPages());

        return new VenueSearchResultVO(result, searchPage.getTotalPages());
    }
}
