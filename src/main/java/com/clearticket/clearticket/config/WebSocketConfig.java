package com.clearticket.clearticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트로 브로드캐스트할 때 사용할 prefix
        registry.enableSimpleBroker("/topic");
        // 클라이언트 -> 서버로 메시지 보낼 때 사용할 prefix (지금은 구독만 쓰지만 확장 대비)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 좌석 실시간 채널 진입점. SockJS fallback 포함
        registry.addEndpoint("/ws-seats")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addEndpoint("/ws-ticket")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}