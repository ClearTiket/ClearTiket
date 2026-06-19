package com.clearticket.clearticket.model.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindPasswordRequestDto {
    private String email;
    private String verifyType; // "name" or "phone"
    private String verifyValue;
}