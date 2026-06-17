package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class MyPageAddressSaveRequestDto {
    private String addressName; // 배송지명 (예: 집, 회사)
    private String recipientName; // 받는 분 이름
    private String recipientPhone; // 연락처
    private String zonecode; // 우편번호
    private String roadAddress; // 도로명 주소
    private String detailAddress; // 상세 주소
    private boolean isDefault; // 기본 배송지 여부
}