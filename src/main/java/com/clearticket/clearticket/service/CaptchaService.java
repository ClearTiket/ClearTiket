package com.clearticket.clearticket.service;

import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CaptchaService { // @SessionScope는 세션별 빈 생성으로 복잡해질 수 있어 제거 추천

    public String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        return IntStream.range(0, length)
                .mapToObj(i -> String.valueOf(chars.charAt(random.nextInt(chars.length()))))
                .collect(Collectors.joining());
    }
}
