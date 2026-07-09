package com.clearticket.clearticket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqChatbotService {

    private final SeatRedisService seatRedisService;

    @Value("${groq.api-key}")
    private String apiKey;

    private final String baseUrl = "https://api.groq.com/openai/v1/chat/completions";
    private final String modelName = "llama3-8b-8192";

    public String getRecommendation(Long performanceId, String userMessage) {
        log.info("🤖 Groq AI 추천 요청 수신 - 공연 ID: {}, 메시지: {}", performanceId, userMessage);

        boolean isSectionASoldOut = seatRedisService.isSectionSoldOut(performanceId, "A구역");

        String soldOutStatusContext = isSectionASoldOut
                ? "현재 [A구역]은 완전히 매진된 상태입니다. 유저에게는 B구역이나 C구역을 대안으로 추천해야 합니다."
                : "현재 모든 구역의 잔여 좌석이 여유로운 상태입니다.";

        String systemInstructions = """
                너는 티켓 예매 플랫폼 'ClearTicket'의 실시간 좌석 안내 및 추천 전문 AI 비러브드 챗봇이야.
                아래의 실시간 매진 컨텍스트 정보를 바탕으로 유저의 질문에 친절하고 똑똑하게 답변해줘.
                
                [실시간 매진 정보]
                %s
                
                [답변 원칙]
                1. 절대로 매진된 구역을 추천해서는 안 돼.
                2. 유저가 대체 구역을 물어보면 매진되지 않은 다른 좋은 구역(예: B구역은 시야가 좋음, C구역은 가성비가 좋음 등)을 제안해줘.
                3. 친근하고 전문적인 어조로 한국어로 답변해줘. 답변은 너무 길지 않게 3~4줄 내외로 간결하게 작성해줘.
                """.formatted(soldOutStatusContext);

        try {
            RestClient restClient = RestClient.builder().build();

            Map<String, Object> requestBody = Map.of(
                    "model", this.modelName,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstructions),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.7
            );

            Map<?, ?> response = restClient.post()
                    .uri(this.baseUrl)
                    .header("Authorization", "Bearer " + this.apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("choices") != null) {
                List<?> choices = (List<?>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                    if (message != null && message.get("content") != null) {
                        String aiResponse = message.get("content").toString();
                        log.info("Groq AI 답변 생성 완료");
                        return aiResponse;
                    }
                }
            }
            return "추천 답변을 가져오는 데 실패했습니다.";
        } catch (Exception e) {
            log.error("Groq AI 직접 통신 중 에러 발생: ", e);
            return "죄송합니다. 현재 실시간 추천 서비스 서버가 혼잡합니다. 잠시 후 다시 시도해 주세요.";
        }
    }
}