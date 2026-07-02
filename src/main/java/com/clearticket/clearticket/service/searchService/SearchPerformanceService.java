package com.clearticket.clearticket.service.searchService;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import com.clearticket.clearticket.model.document.PerformanceDocument;
import com.clearticket.clearticket.model.dto.SearchPerformanceFilterDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchPerformanceService {

    final ElasticsearchOperations elasticsearchOperations;

    /**
     * 공연 제목 검색
     *
     * @param searchText 사용자가 입력한 검색어
     * @return 검색 결과 List
     */
    public List<PerformanceDocument> searchPerformances(String searchText, SearchPerformanceFilterDto filterDto) {

        // 공백 기준 검색어를 키워드로 부리
        List<String> keywords = Arrays.stream(searchText.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .toList();

        // 검색어가 없거나 공백만 존재하는 경우 빈 List 반환
        if (keywords.isEmpty()) return new ArrayList<>();

        List<Query> mustQueries = new ArrayList<>(); // 모두 만족해야 하는 조건들
        List<Query> shouldQueries = new ArrayList<>(); // 하나라도 만족하면 통과인 조건들

        // 분리된 검색어를 must query에 입력
        for (String keyword : keywords) {
            mustQueries.add(Query.of(
                    q -> q.match(m -> m.field("title").query(keyword))));
        }

        // 장르 검색 필터링
        mustQueries.add(Query.of(q -> q.terms(
                t -> t
                        .field("genre")
                        .terms(TermsQueryField.of(v -> v.value(filterDto.getTagsGenre()
                                .stream()
                                .map(FieldValue::of)
                                .collect(Collectors.toList()))))
        )));

        // 분위기 태그 검색 필터링
        mustQueries.add(Query.of(q -> q.terms(
                t -> t
                        .field("tags_vibe")
                        .terms(TermsQueryField.of(v -> v.value(filterDto.getTagsVibe()
                                .stream()
                                .map(FieldValue::of)
                                .collect(Collectors.toList()))))
        )));

        // 동행 태그 검색 필터링
        mustQueries.add(Query.of(q -> q.terms(
                t -> t
                        .field("tags_with")
                        .terms(TermsQueryField.of(v -> v.value(filterDto.getTagsWith()
                                .stream()
                                .map(FieldValue::of)
                                .collect(Collectors.toList()))))
        )));

        // 공연 상태 검색 필터링
        mustQueries.add(Query.of(q -> q.terms(
                t -> t
                        .field("status")
                        .terms(TermsQueryField.of(v -> v.value(filterDto.getStatuses()
                                .stream()
                                .map(FieldValue::of)
                                .collect(Collectors.toList()))))
        )));

        // 공연 지역 검색 필터링
        mustQueries.add(Query.of(q -> q.terms(
                t -> t
                        .field("region")
                        .terms(TermsQueryField.of(v -> v.value(filterDto.getRegions()
                                .stream()
                                .map(FieldValue::of)
                                .collect(Collectors.toList()))))
        )));

        // 오늘과 가까운 날짜일수록 가중치 부여
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Query dateBoostQuery = Query.of(q -> q
                .distanceFeature(df -> df
                        .date(d -> d
                                .field("start_date")
                                .origin(todayStr)
                                .pivot(Time.of(t -> t.time("7d")))
                                .boost(2.0f))
                )
        );

        shouldQueries.add(dateBoostQuery);


        Query endedFilterQuery = Query.of(q -> q.bool(
                b -> b.filter(
                        f -> f.range(
                                r -> r.date(
                                        d -> d.field("end_date").gte(todayStr)
                                )
                        )
                )
        ));

        mustQueries.add(endedFilterQuery);

        // 최종 bool 쿼리 생성
        Query finalBoolQuery = Query.of(
                q -> q.bool(b -> b.must(mustQueries).should(shouldQueries)));

        // native query를 통해 전송 규격으로 래핑
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(finalBoolQuery)
                .withPageable(PageRequest.of(0, 20)) // 검색 결과 점수 상위 20개
                .build();

        SearchHits<PerformanceDocument> searchHits = elasticsearchOperations.search(nativeQuery, PerformanceDocument.class);


        var result = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        System.out.println(result.size());

        return result;
    }


}
