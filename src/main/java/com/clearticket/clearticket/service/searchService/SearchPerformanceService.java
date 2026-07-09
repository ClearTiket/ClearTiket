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

        // 공백 기준 검색어를 키워드로 분리
        List<String> keywords = Arrays.stream(searchText.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .toList();

        // 검색어가 없거나 공백만 존재하는 경우 빈 List 반환
        if (keywords.isEmpty()) return new ArrayList<>();

        List<Query> mustQueries = new ArrayList<>();   // 모두 만족해야 하는 조건들
        List<Query> shouldQueries = new ArrayList<>();  // 하나라도 만족하면 통과인 조건들

        // 분리된 검색어를 must query에 입력
        for (String keyword : keywords) {
            mustQueries.add(Query.of(
                    q -> q.match(m -> m.field("title").query(keyword))));
        }

        // 버그 수정: 아래 4개 필터(장르/분위기/동행/판매상태/지역)는 전부
        // "선택된 값이 하나도 없으면 필터를 걸지 않는다(=전체 보기)"가 의도된 동작이다.
        // 예전 코드는 filterDto가 빈 리스트일 때도 항상 terms 쿼리를 must에 추가했는데,
        // ES의 terms 쿼리는 "values"가 비어있으면 무조건 매칭 결과가 0건이 되기 때문에
        // (그리고 DTO가 빈 리스트를 기본값으로 치환하더라도, tags_vibe/tags_with처럼
        //  문서 자체의 배열 필드가 비어있는 경우엔 그 기본값 terms조차 매칭이 안 됐다)
        // 결과적으로 필터를 하나도 안 건드리고 재검색만 눌러도 결과가 사라지는 문제가 있었다.
        // 그래서 이제는 리스트가 비어있을 때는 아예 조건을 추가하지 않는다.
        addTermsFilterIfPresent(mustQueries, "genre", filterDto.getTagsGenre());
        addTermsFilterIfPresent(mustQueries, "tags_vibe", filterDto.getTagsVibe());
        addTermsFilterIfPresent(mustQueries, "tags_with", filterDto.getTagsWith());
        addTermsFilterIfPresent(mustQueries, "region", filterDto.getRegions());

        if (filterDto.getStatuses() != null && !filterDto.getStatuses().isEmpty()) {
            List<FieldValue> statusValues = filterDto.getStatuses().stream()
                    .map(status -> FieldValue.of(status.name()))
                    .collect(Collectors.toList());
            mustQueries.add(Query.of(q -> q.terms(
                    t -> t.field("status").terms(TermsQueryField.of(v -> v.value(statusValues))))));
        }

        // 오늘과 가까운 날짜일수록 가중치 부여
        // ES에 색인된 start_date/end_date는 파이썬(elasticsearch-py)이 date 객체를 그대로 직렬화한
        // "yyyy-MM-dd" 형식(strict_date_optional_time)이므로, 쿼리도 반드시 같은 포맷을 써야 한다.
        // 기존 "yyyyMMdd" 포맷(예: 20260708)은 ES 기본 date 포맷과 맞지 않아
        // 모든 샤드에서 날짜 파싱에 실패해 500(all shards failed) 에러가 발생했었음.
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // pivot()은 람다가 아니라 완성된 Time 객체를 직접 받음
        Time pivotTime = Time.of(t -> t.time("7d"));
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

        // 시작/종료 날짜 필터링
        // 필터 날짜의 범위에 공연 시작-종료 범위가 겹쳐져 있는(걸치는) 경우 검색
        //
        // 중요: 사용자가 날짜 선택기를 실제로 조작해서 startDate/endDate를 "둘 다" 넘겨줬을 때만
        // 날짜 범위 필터를 적용한다. 하나라도 null이면(=사용자가 날짜를 고르지 않았으면)
        // 날짜 조건 없이 다른 필터(장르/지역/판매상태 등)만으로 검색한다.
        //
        // 예전에는 startDate/endDate가 항상 "오늘 ~ 오늘+13일"로 강제 세팅돼서,
        // 재검색 버튼만 눌러도 그 좁은 날짜 범위 밖의 공연은 전부 결과에서 사라지는 문제가 있었다.
        if (filterDto.getStartDate() != null && filterDto.getEndDate() != null) {
            List<Query> dateFilters = new ArrayList<>();

            String startDateStr = filterDto.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String endDateStr = filterDto.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 공연 시작 날짜 <= 필터 종료 날짜
            dateFilters.add(Query.of(q -> q.range(r -> r.date(d ->
                    d.field("start_date").lte(endDateStr)))));
            // 공연 종료 날짜 >= 필터 시작 날짜
            dateFilters.add(Query.of(q -> q.range(r -> r.date(d ->
                    d.field("end_date").gte(startDateStr)))));

            Query dateFilterQuery = Query.of(q -> q.bool(b -> b.filter(dateFilters)));
            mustQueries.add(dateFilterQuery);
        }

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

        // 진단용 로그: 재검색 결과가 이상할 때 어떤 필터가 실제로 걸렸는지,
        // must 조건이 몇 개나 쌓였는지, 최종 몇 건이 나왔는지 바로 확인 가능하게 남겨둔다.
        log.info("[재검색 결과] must조건 수={}, genre={}, vibe={}, with={}, region={}, status={}, 결과 {}건",
                mustQueries.size(),
                filterDto.getTagsGenre(), filterDto.getTagsVibe(), filterDto.getTagsWith(),
                filterDto.getRegions(), filterDto.getStatuses(), result.size());

        return result;
    }

    /**
     * values가 비어있지 않을 때만 해당 필드에 대한 terms(=OR) 조건을 mustQueries에 추가한다.
     * values가 비어있으면(=사용자가 아무것도 선택하지 않았으면) 그 필드는 검색 조건에서 제외한다.
     */
    private void addTermsFilterIfPresent(List<Query> mustQueries, String field, List<?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<FieldValue> fieldValues = values.stream()
                .map(v -> FieldValue.of(String.valueOf(v)))
                .collect(Collectors.toList());
        mustQueries.add(Query.of(q -> q.terms(
                t -> t.field(field).terms(TermsQueryField.of(v -> v.value(fieldValues))))));
    }
}