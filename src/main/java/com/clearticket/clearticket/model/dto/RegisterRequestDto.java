package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDto {
    private String email;
    private String password;
    private String name;
    private String phone;
    private String address;
}