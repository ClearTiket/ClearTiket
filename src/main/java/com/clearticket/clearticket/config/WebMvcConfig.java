package com.clearticket.clearticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginUserInterceptor())
                // 정적 리소스(css/js/images)에는 적용할 필요 없음
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/favicon.ico");
    }
}