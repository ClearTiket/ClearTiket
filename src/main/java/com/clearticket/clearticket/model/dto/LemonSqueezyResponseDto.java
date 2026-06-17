package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class LemonSqueezyResponseDto {
    private String checkoutUrl;    // 사용자가 접속할 레몬스퀴지 가상 결제창 주소
}