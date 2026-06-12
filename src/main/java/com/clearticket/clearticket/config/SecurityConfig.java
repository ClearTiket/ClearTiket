package com.clearticket.clearticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.web.servlet.ServletContextInitializer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // 세션 관리: 30분 비활동 시 만료
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login?expired=true")
                        .maximumSessions(1)
                        .expiredUrl("/login?expired=true")
                );

        return http.build();
    }

    /**
     * 세션 타임아웃 30분 설정 (1800초)
     * application.properties 에 server.servlet.session.timeout=30m 을 추가해도 동일하게 적용됩니다.
     */
    @Bean
    public ServletContextInitializer sessionTimeoutInitializer() {
        return servletContext -> servletContext.setSessionTimeout(30); // 단위: 분
    }
}