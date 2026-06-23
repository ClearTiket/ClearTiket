package com.clearticket.clearticket.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReservationBuyerInfoRequestDto {
    private String ticketType; // 수령 방식 ("VENUE" 또는 "DELIVERY")
    private int shippingFee; // 배송비 (0원 또는 4000원)
    private String recipientName; // 수령인(주문자) 이름
    private String recipientPhone; // 수령인(주문자) 연락처
    private int totalPrice; // 화면에서 배송비까지 최종 합산된 총 결제 금액
}
