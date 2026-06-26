package com.clearticket.clearticket.model.entity;

public enum PaymentMethod {
    BANK_TRANSFER, // 무통장입금 (가상계좌)
    CARD,          // 신용/체크카드 (레몬스퀴지 기본)
    E_WALLET       // 간편결제 (카카오페이, 토스페이, 페이팔 등)
}