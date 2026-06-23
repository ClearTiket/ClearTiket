package com.clearticket.clearticket.model.dto;

import lombok.*;

@Getter
@Setter
@ToString
public class PaymentRequestDto {
    private Long reservationId;
    private String paymentMethod; // 결제수단 (CARD, BANK_TRANSFER, EASY_PAYMENT)
    private String bankName;
    private int amount;
}