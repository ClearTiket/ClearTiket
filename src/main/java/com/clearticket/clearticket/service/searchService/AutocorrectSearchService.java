package com.clearticket.clearticket.service.searchService;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.clearticket.clearticket.model.document.PerformanceDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutocorrectSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 4가지 필드(공연명, 배우, 장르, 공연장명) 통합 오타 교정 검색
     * @param searchText 사용자가 입력한 검색 텍스트 (Null 또는 공백일 경우 빈 리스트 반환)
     * @return 오타 교정 및 필드 조건이 반영되어 매칭된 결과
     */
    public List<PerformanceDocument> searchAutocorrect(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        final String keyword = searchText.trim();

        Query integrationQuery = Query.of(q -> q
                .bool(b -> b
                        // 제목(title) 매칭 조건 (오타 허용 + 부분 일치 둘 다 커버)
                        .should(s -> s.match(m -> m.field("title").query(keyword).fuzziness("AUTO")))
                        .should(s -> s.wildcard(w -> w.field("title").value("*" + keyword + "*")))

                        // 배우명(castings) 매칭 조건 (정확히 검색 또는 포함될 때만 - 오타 교정 X)
                        .should(s -> s.match(m -> m.field("castings").query(keyword)))
                        .should(s -> s.wildcard(w -> w.field("castings").value("*" + keyword + "*")))

                        // 장르명(genre) 매칭 조건 (정확히 검색 - 오타 교정 X)
                        .should(s -> s.match(m -> m.field("genre").query(keyword)))

                        // 공연장명(venue_name) 매칭 조건 (선택 사항 - 오타 교정 X)
                        .should(s -> s.match(m -> m.field("venue_name").query(keyword)))
                        .should(s -> s.wildcard(w -> w.field("venue_name").value("*" + keyword + "*")))
                )
        );

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(integrationQuery)
                .withPageable(PageRequest.of(0, 100))
                .build();

        SearchHits<PerformanceDocument> searchHits = elasticsearchOperations.search(nativeQuery, PerformanceDocument.class);

        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }


    /**
     * 애플리케이션 구동 완료 시점에 PostgreSQL RDB와 Elasticsearch 서버 간의 데이터를 동기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncPostgresToElasticsearch() {
        log.info("🚀 [통합 동기화] PostgreSQL (performances + venues) -> 엘라스틱서치 복사를 시작합니다...");

        String sql = "SELECT p.performance_id, p.title, p.genre, p.region, p.status, " +
                "to_char(p.start_date, 'YYYY-MM-DD') as start_date, " +
                "to_char(p.end_date, 'YYYY-MM-DD') as end_date, " +
                "p.castings, p.poster_url, p.extracted_text, " +
                "v.name as venue_name, " +
                "COALESCE(" + // 분위기 태그 배열 join
                "  array_agg(t.tag_id) " +
                "    FILTER (WHERE t.tag_id BETWEEN 201 AND 209), " +
                "  '{}'" +
                ") AS tags_vibe, " +
                "COALESCE(" + // 동행 태그 배열 join
                "  array_agg(t.tag_id) " +
                "    FILTER (WHERE t.tag_id BETWEEN 301 AND 305), " +
                "  '{}'" +
                ") AS tags_with " +
                "FROM performances p " +
                "LEFT JOIN venues v ON p.venue_id = v.venue_id " + // 외래키 연결 조인
                "LEFT JOIN performance_tags t ON p.performance_id = t.performance_id " +
                "GROUP BY p.performance_id, v.name";

        try {
            List<PerformanceDocument> dbData = jdbcTemplate.query(sql, (rs, rowNum) -> {
                PerformanceDocument doc = new PerformanceDocument();

                doc.setPerformanceId(rs.getLong("performance_id"));
                doc.setTitle(rs.getString("title"));
                doc.setGenre(rs.getString("genre"));
                doc.setRegion(rs.getString("region"));
                doc.setStatus(rs.getString("status"));
                doc.setStartDate(rs.getString("start_date"));
                doc.setEndDate(rs.getString("end_date"));
                doc.setCastings(rs.getString("castings"));
                doc.setPosterUrl(rs.getString("poster_url"));
                doc.setExtractedText(rs.getString("extracted_text"));

                doc.setVenueName(rs.getString("venue_name"));

                doc.setTagsVibe(java.util.Arrays.asList((Integer[]) rs.getArray("tags_vibe").getArray()));
                doc.setTagsWith(java.util.Arrays.asList((Integer[]) rs.getArray("tags_with").getArray()));

                return doc;
            });

            if (dbData.isEmpty()) {
                log.warn("⚠️ PostgreSQL DB에 가져올 데이터가 없습니다.");
                return;
            }

            log.info("📦 조인된 총 {}개의 데이터를 엘라스틱서치로 전송합니다...", dbData.size());

            elasticsearchOperations.save(dbData);

            log.info("✅ [동기화 완료] 4대 필드가 포함된 데이터가 performances 방에 세팅되었습니다!");

        } catch (Exception e) {
            log.error("❌ 데이터 조인 및 복사 중 에러 발생: ", e);
        }
    }

    /**
     * 엘라스틱서치를 이용한 실시간 검색어 자동완성 및 오타 교정 제안 API
     * @param searchText 유저가 검색창에 실시간으로 입력 중인 검색어 문자열
     * @return 검색어와 매칭 및 오타 교정된 공연 제목 리스트
     */
    public List<String> getSuggestions(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Query suggestQuery = Query.of(q -> q
                    .multiMatch(mm -> mm
                            .fields("title")
                            .query(searchText)
                            .fuzziness("AUTO") // 오타 교정 기능 포함
                    )
            );

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(suggestQuery)
                    .withPageable(PageRequest.of(0, 5)) // 추천 검색어는 상위 5개만 보여주는 게 국룰!
                    .build();

            SearchHits<PerformanceDocument> searchHits = elasticsearchOperations.search(nativeQuery, PerformanceDocument.class);

            return searchHits.stream()
                    .map(hit -> hit.getContent().getTitle())
                    .distinct()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Suggestion API 실행 중 에러 발생: ", e);
            return new ArrayList<>();
        }
    }
}