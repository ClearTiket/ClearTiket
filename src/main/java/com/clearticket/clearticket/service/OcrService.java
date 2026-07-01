package com.clearticket.clearticket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class OcrService {

    @Value("${naver.ocr.invoke-url}")
    private String invokeUrl;

    @Value("${naver.ocr.secret-key}")
    private String secretKey;

    public String callOcr(String imageUrl) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-OCR-SECRET", secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 요청 바디 설정 (이미지 정보 포함)
        long timestamp = System.currentTimeMillis();
        String requestBody = String.format("""
            {
                "version": "V2",
                "requestId": "req-%d",
                "timestamp": %d,
                "images": [{"format": "png", "name": "poster", "url": "%s"}]
            }
            """, timestamp, timestamp, imageUrl);

        // 3. API 호출
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(invokeUrl, entity, String.class);

        return response.getBody();
    }
    // 응답 파싱
    public String extractTextFromOcr(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonResponse);

            // 1. 네이버 OCR 응답의 핵심 구조: 'images' 배열 안에 'fields'가 들어있음
            JsonNode images = root.path("images");
            StringBuilder fullText = new StringBuilder();

            for (JsonNode image : images) {
                JsonNode fields = image.path("fields");
                for (JsonNode field : fields) {
                    // 2. 'inferText' 값을 정확히 추출
                    String text = field.path("inferText").asText();
                    if (text != null && !text.isEmpty()) {
                        fullText.append(text).append(" ");
                    }
                }
            }

            String result = fullText.toString();
            System.out.println(">>> [DEBUG] 최종 추출된 텍스트 전체: " + result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "텍스트 추출 실패";
        }
    }
    
}
