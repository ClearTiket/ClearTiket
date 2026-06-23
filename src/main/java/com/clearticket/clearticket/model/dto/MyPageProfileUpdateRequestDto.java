package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPageProfileUpdateRequestDto {
    private String name; // 수정할 이름
    private String email; // 수정할 이메일
    private String phone; // 수정할 전화번호
}