package com.clearticket.clearticket.service.searchService;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.TimeUnit;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.clearticket.clearticket.model.document.PerformanceDocument;
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
    public List<PerformanceDocument> searchPerformances(String searchText) {

        // 공백 기준 검색어를 키워드로 분리
        List<String> keywords = Arrays.stream(searchText.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .toList();

        // 검색어가 없거나 공백만 존재하는 경우 빈 List 반환
        if (keywords.isEmpty()) return new ArrayList<>();

        List<Query> mustQueries = new ArrayList<>();   // 모두 만족해야 하는 조건들
        List<Query> shouldQueries = new ArrayList<>();  // 하나라도 만족하면 통과인 조건들

        for (String keyword : keywords) {
            mustQueries.add(Query.of(
                    q -> q.match(m -> m.field("title").query(keyword))));
        }

        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // pivot()은 람다가 아니라 완성된 Time 객체를 직접 받음
        Time pivotTime = Time.of(t -> t.time(7, TimeUnit.Days));

        // boost()는 .date(...) 바깥이 아니라 d 람다 안에서 설정해야 함
        Query dateBoostQuery = Query.of(q -> q
                .distanceFeature(df -> df
                        .date(d -> d
                                .field("start_date")
                                .origin(todayStr)
                                .pivot(pivotTime)
                                .boost(2.0f)
                        )
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

        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

}