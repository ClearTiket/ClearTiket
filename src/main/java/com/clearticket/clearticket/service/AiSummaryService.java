package com.clearticket.clearticket.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OCR로 추출된 포스터 원문(extractedText)을 OpenAI(GPT) 또는 Gemini로 3줄 요약하는 서비스.
 * application.yml 의 ai.summary.provider 값(openai / gemini)에 따라 사용할 공급자를 결정한다.
 *
 * 필요한 설정 (application.yml 또는 config.yml / 환경변수):
 *   ai.summary.provider = openai | gemini
 *   openai.api-key      = sk-xxxx...
 *   openai.model        = gpt-4o-mini (기본값)
 *   gemini.api-key       = AIza...
 *   gemini.model         = gemini-1.5-flash (기본값)
 */
@Slf4j
@Service
public class AiSummaryService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.summary.provider:openai}")
    private String provider;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    private static final String PROMPT_TEMPLATE = """
            다음은 공연 포스터 이미지를 OCR로 인식한 원문 텍스트입니다.
            핵심 정보(공연명, 기간, 장소, 주요 특징 등)만 담아 한국어로 정확히 3줄로 요약해 주세요.
            각 줄은 "- " 로 시작하고, 광고 문구나 의미 없는 특수문자는 제외해 주세요.
            결과는 3줄 텍스트만 출력하고, 다른 설명은 붙이지 마세요.

            [OCR 원문]
            %s
            """;

    /**
     * OCR 원문 텍스트를 받아 3줄 요약 문자열을 반환한다.
     * provider 설정값에 따라 OpenAI 또는 Gemini를 호출한다.
     */
    public String getSummaryFromText(String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            return "요약할 원문 텍스트가 없습니다.";
        }

        String prompt = PROMPT_TEMPLATE.formatted(extractedText);

        try {
            if ("gemini".equalsIgnoreCase(provider)) {
                return callGemini(prompt);
            }
            return callOpenAi(prompt);
        } catch (Exception e) {
            log.error("AI 3줄 요약 생성 실패 (provider={})", provider, e);
            return "요약 생성 중 오류가 발생했습니다.";
        }
    }

    // ── OpenAI (GPT) ─────────────────────────────────────────────
    private String callOpenAi(String prompt) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("openai.api-key 설정이 비어있습니다.");
        }

        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", openAiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", "너는 공연 포스터 OCR 원문을 3줄로 핵심 요약하는 어시스턴트야."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            return content.trim();
        } catch (HttpClientErrorException e) {
            log.error("OpenAI 호출 실패({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("OpenAI API 호출에 실패했습니다: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI 응답 처리 중 오류가 발생했습니다.", e);
        }
    }

    // ── Google Gemini ────────────────────────────────────────────
    private String callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("gemini.api-key 설정이 비어있습니다.");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s"
                .formatted(geminiModel, geminiApiKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text").asText("");
            return content.trim();
        } catch (HttpClientErrorException e) {
            log.error("Gemini 호출 실패({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Gemini API 호출에 실패했습니다: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Gemini 응답 처리 중 오류가 발생했습니다.", e);
        }
    }
}
