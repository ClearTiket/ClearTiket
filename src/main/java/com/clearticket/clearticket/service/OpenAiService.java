package com.clearticket.clearticket.service;

import org.springframework.stereotype.Service;

@Service
public class OpenAiService {
    // OpenAi 요약
    // RestTemplate을 이용해 API를 호출합니다.
    public String getSummaryFromText(String extractedText) {
        // OpenAI API 호출 및 요약 결과 반환 로직
        return "요약된 내용";
    }
}
