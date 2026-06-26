package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindEmailRequestDto {
    private String type;   // "name" | "phone"
    private String name;
    private String birth;
    private String phone;
}
