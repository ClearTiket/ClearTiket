package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPageAddressResponseDto {
    private Long addressId; // 주소 고유 번호
    private String addressName; // 배송지명 (예: 우리집, 회사)
    private String recipientName; // 받는 사람 이름
    private String recipientPhone; // 받는 사람 연락처
    private String zonecode; // 우편번호 (5자리)
    private String roadAddress; // 도로명 주소
    private String detailAddress; // 상세 주소
    private boolean isDefault; // 기본 배송지 여부 (true/false)
}