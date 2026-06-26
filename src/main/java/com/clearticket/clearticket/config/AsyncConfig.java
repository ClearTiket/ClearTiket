package com.clearticket.clearticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 이메일 발송용 비동기 스레드풀 설정.
 * 인증번호 발송 API가 SMTP 응답을 기다리지 않고 즉시 응답할 수 있도록
 * 메일 전송을 별도 스레드로 분리한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }
}