package com.clearticket.clearticket.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class GroqAiConfig {

    @Value("${groq.api-key}")
    private String apiKey;

    private final String baseUrl = "https://api.groq.com/openai/v1";
    private final String modelName = "llama3-8b-8192";

    @Bean
    public OpenAiChatModel openAiChatModel() {
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            throw new IllegalStateException("[ClearTicket 에러] config.yml 파일에서 'groq.api-key'를 읽어오지 못했습니다.");
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(this.baseUrl)
                .apiKey(this.apiKey)
                .build();

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model(this.modelName)
                .temperature(0.7)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();
    }
}