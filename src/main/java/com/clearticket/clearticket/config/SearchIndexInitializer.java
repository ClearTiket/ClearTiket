package com.clearticket.clearticket.config;

import com.clearticket.clearticket.service.searchService.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 기동 시 Elasticsearch의 venues / performances 인덱스가 존재하는지 확인하고,
 * 없으면 자동으로 생성 및 DB 데이터 색인을 수행한다.
 *
 * 기존에는 이 로직이 전혀 없어서 ES가 초기화된 환경(신규 배포, 컨테이너 재시작 등)에서는
 * "Index venues not found" / "Index performances not found" 오류로 메인 검색, 공연장 검색이 항상 실패했다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexInitializer implements ApplicationRunner {

    private final SearchIndexService searchIndexService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            searchIndexService.reindexAllIfMissing();
        } catch (Exception e) {
            // ES가 아직 켜지지 않았거나 연결에 실패해도 앱 자체는 정상 기동되어야 하므로 예외를 흡수하고 로그만 남긴다.
            log.error("[검색색인] 초기 색인 중 오류가 발생했습니다. Elasticsearch 연결 상태를 확인해주세요.", e);
        }
    }
}
