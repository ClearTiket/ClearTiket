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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 네이버 클라우드 플랫폼 CLOVA OCR(General) API 연동 서비스.
 * - callOcr(imageUrl) : 이미지 URL을 CLOVA OCR로 전달하여 인식 결과(JSON 원본)를 받아온다.
 * - extractTextFromOcr(rawJson) : OCR 응답 JSON에서 인식된 글자만 순서대로 이어붙인 순수 텍스트로 변환한다.
 *
 * 사전 준비 (Naver Cloud Console):
 * 1) AI Services > CLOVA OCR > Domain 에서 "General" 도메인 생성
 * 2) API Gateway 자동연동 → Secret Key 발급, Invoke URL 확인
 * 3) application.yml (또는 config.yml) 에 아래 값 채우기
 *      naver:
 *        ocr:
 *          invoke-url: https://xxxx.apigw.ntruss.com/custom/v1/xxxx/xxxx/general
 *          secret-key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 */
@Slf4j
@Service
public class OcrService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${naver.ocr.invoke-url:}")
    private String invokeUrl;

    @Value("${naver.ocr.secret-key:}")
    private String secretKey;

    /**
     * 이미지 URL을 네이버 CLOVA OCR(General)에 전달하여 인식 결과 JSON 원문을 그대로 반환한다.
     */
    public String callOcr(String imageUrl) {
        if (invokeUrl == null || invokeUrl.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("naver.ocr.invoke-url / naver.ocr.secret-key 설정이 비어있습니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-OCR-SECRET", secretKey);

        Map<String, Object> image = new HashMap<>();
        image.put("format", resolveImageFormat(imageUrl));
        image.put("name", "poster");
        image.put("url", imageUrl); // 이미지가 이미 외부에서 접근 가능한 URL이므로 base64 변환 없이 url로 전달

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("version", "V2");
        requestBody.put("requestId", UUID.randomUUID().toString());
        requestBody.put("timestamp", System.currentTimeMillis());
        requestBody.put("lang", "ko");
        requestBody.put("images", List.of(image));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("네이버 CLOVA OCR 호출 시작 - imageUrl: {}", imageUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(invokeUrl, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("CLOVA OCR 호출 실패({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("CLOVA OCR 요청이 거부되었습니다: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("CLOVA OCR 호출 중 예외 발생 - imageUrl: {}", imageUrl, e);
            throw new IllegalStateException("CLOVA OCR 서버 호출에 실패했습니다.", e);
        }
    }

    /**
     * 확장자를 보고 CLOVA OCR이 요구하는 format 값을 추정한다. (jpg, jpeg, png, gif, bmp, pdf, tiff 등 지원)
     * 확장자를 알 수 없으면 기본값으로 jpg를 사용한다.
     */
    private String resolveImageFormat(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        int q = lower.indexOf('?');
        if (q > -1) lower = lower.substring(0, q); // 쿼리스트링 제거 후 확장자 판별

        if (lower.endsWith(".png")) return "png";
        if (lower.endsWith(".jpeg") || lower.endsWith(".jpg")) return "jpg";
        if (lower.endsWith(".gif")) return "gif";
        if (lower.endsWith(".bmp")) return "bmp";
        if (lower.endsWith(".tif") || lower.endsWith(".tiff")) return "tiff";
        if (lower.endsWith(".pdf")) return "pdf";
        return "jpg";
    }

    /**
     * CLOVA OCR General 응답 JSON을 파싱하여, 인식된 텍스트(fields[].inferText)를
     * 읽는 순서대로 이어붙인 순수 텍스트로 변환한다.
     * lineBreak=true 인 필드 뒤에는 줄바꿈을, 아니면 공백을 넣어 자연스러운 문단으로 만든다.
     */
    public String extractTextFromOcr(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode imagesNode = root.path("images");

            StringBuilder sb = new StringBuilder();
            for (JsonNode imageNode : imagesNode) {
                // 인식 실패(FAILURE) 이미지는 건너뜀
                String inferResult = imageNode.path("inferResult").asText("");
                if ("ERROR".equalsIgnoreCase(inferResult)) {
                    log.warn("CLOVA OCR 인식 실패 이미지 발견, message={}", imageNode.path("message").asText(""));
                    continue;
                }

                JsonNode fields = imageNode.path("fields");
                for (JsonNode field : fields) {
                    String text = field.path("inferText").asText("");
                    if (text.isBlank()) continue;

                    sb.append(text);
                    boolean lineBreak = field.path("lineBreak").asBoolean(false);
                    sb.append(lineBreak ? "\n" : " ");
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("CLOVA OCR 응답 파싱 실패. rawJson={}", rawJson, e);
            return "";
        }
    }
}
