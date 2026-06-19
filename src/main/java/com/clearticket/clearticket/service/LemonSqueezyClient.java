package com.clearticket.clearticket.service; // 프로젝트 패키지 구조에 맞게 수정하세요.

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 레몬스퀴지(Lemon Squeezy) 외부 API 서버와의 HTTP 통신을 전담하는 클라이언트 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LemonSqueezyClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${lemonsqueezy.api-key}")
    private String apiKey;

    @Value("${lemonsqueezy.store-id}")
    private String storeId;

    @Value("${lemonsqueezy.variant-id}")
    private String baseVariantId;

    /**
     * 레몬스퀴지 API를 호출하여 동적 금액(Custom Price)과 상품명이 적용된 체크아웃 URL 발급
     *
     * @param realPrice DB에서 조회한 실제 티켓 결제 금액
     * @param checkoutName 결제창에 표시할 공연명 및 좌석 등급 정보
     * @param reservationId 가등록된 예약 고유 ID
     * @return 레몬스퀴지 서버에서 동적으로 생성된 결제창 URL 주소
     */
    public String requestDynamicCheckoutUrl(int realPrice, String checkoutName, Long reservationId) {
        String apiUrl = "https://api.lemonsqueezy.com/v1/checkouts";

        // 1. HTTP Header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.api+json"));
        headers.setAccept(java.util.List.of(MediaType.valueOf("application/vnd.api+json")));
        headers.setBearerAuth(apiKey);

        // 2. 레몬스퀴지 규격(JSON:API) 요청 바디 조립
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> productOptions = new HashMap<>();
        Map<String, Object> checkoutData = new HashMap<>();
        Map<String, Object> customData = new HashMap<>();

        data.put("type", "checkouts");

        // 금액 바인딩
        attributes.put("custom_price", realPrice * 100);

        // 상품명 주입
        productOptions.put("name", checkoutName);
        attributes.put("product_options", productOptions);

        // 예약 ID 주입 (문자열로 변환)
        customData.put("reservation_id", String.valueOf(reservationId));
        checkoutData.put("custom", customData);
        attributes.put("checkout_data", checkoutData);

        // attributes를 data에 등록
        data.put("attributes", attributes);

        // [핵심 2] 상점 및 상품 관계(relationships) 정석 구조 조립
        Map<String, Object> relationships = new HashMap<>();

        // store 구조 세팅
        Map<String, Object> store = new HashMap<>();
        Map<String, Object> storeData = new HashMap<>();
        storeData.put("type", "stores");
        storeData.put("id", storeId); // String 변수 그대로 매핑
        store.put("data", storeData);
        relationships.put("store", store);

        // variant 구조 세팅
        Map<String, Object> variant = new HashMap<>();
        Map<String, Object> variantData = new HashMap<>();
        variantData.put("type", "variants");
        variantData.put("id", baseVariantId); // String 변수 그대로 매핑
        variant.put("data", variantData);
        relationships.put("variant", variant);

        // relationships를 data에 등록
        data.put("relationships", relationships);

        // 최종 data를 requestBody에 주입
        requestBody.put("data", data);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("레몬스퀴지 API 호출 시작 - URL: {}, 요청 금액: {}원", apiUrl, realPrice);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> resBody = response.getBody();
                Map<String, Object> resData = (Map<String, Object>) resBody.get("data");
                Map<String, Object> resAttributes = (Map<String, Object>) resData.get("attributes");

                String checkoutUrl = (String) resAttributes.get("url");
                log.info("레몬스퀴지 체크아웃 URL 발급 성공: {}", checkoutUrl);
                return checkoutUrl;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("레몬스퀴지 422 본문 에러 상세: {}", e.getResponseBodyAsString());
            throw new IllegalStateException("결제창 생성 중 외부 플랫폼 연동 에러가 발생했습니다.");
        } catch (Exception e) {
            log.error("레몬스퀴지 API 연동 중 예외 발생: {}", e.getMessage(), e);
            throw new IllegalStateException("결제창 생성 중 외부 플랫폼 연동 에러가 발생했습니다.");
        }
        throw new IllegalStateException("레몬스퀴지 서버로부터 올바른 응답을 받지 못했습니다.");
    }
}