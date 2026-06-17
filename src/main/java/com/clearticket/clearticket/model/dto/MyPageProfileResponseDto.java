package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPageProfileResponseDto {
    private Long userId;
    private String loginId;
    private String name;
    private String email;
    private String phone;
}
