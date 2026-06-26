package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDto {
    private String email;
    private String password;
    private String passwordConfirm;
    private String name;
    private String birthdate;     // 생년월일 8자리 (예: 19990101)
    private String phone;
    private String zipcode;       // 우편번호
    private String address;       // 기본 주소
    private String addressDetail; // 상세 주소
}
