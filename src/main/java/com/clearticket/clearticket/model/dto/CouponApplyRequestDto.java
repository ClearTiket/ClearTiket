package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class CouponApplyRequestDto {
    private String couponId;
    private int totalPrice;
}