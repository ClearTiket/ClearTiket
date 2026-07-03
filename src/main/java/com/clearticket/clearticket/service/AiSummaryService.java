package com.clearticket.clearticket.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiSummaryService {

    private final Client client;
    private final String modelName;

    public AiSummaryService(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash-001}") String modelName) {
        this.client = Client.builder().apiKey(apiKey).build();
        this.modelName = modelName;
    }

    // OpenAi 요약
    // RestTemplate을 이용해 API를 호출합니다.
    public String getSummaryFromText(String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            return "요약할 원문 텍스트가 없습니다.";
        }

        String prompt = """
                다음은 공연 포스터 OCR 원문이야. 
                핵심 정보(줄거리, 개요, 주요 특징) 위주로 3줄 요약해줘.
                규칙:
                - 각 줄은 "- "로 시작할 것.
                - 광고 문구 제외.
                - 딱 3줄 요약 결과만 출력할 것.
                
                [OCR 원문]
                %s
                """.formatted(extractedText);

        try {
            // 제미나이 SDK를 통한 호출
            GenerateContentResponse response = client.models.generateContent(modelName, prompt, null);
            String summary = response.text();

            if (summary == null || summary.isBlank()) {
                return "AI가 요약 결과를 반환하지 않았습니다.";
            }
            System.out.println(">>> [DEBUG] Gemini 3줄 요약 결과: " + summary);
            return summary.trim();
        } catch (Exception e) {
            System.out.println(">>> [ERROR] Gemini 통신 중 상세 에러: " + e.getMessage());
            return "요약 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}