package com.clearticket.clearticket.model.entity;

public enum PaymentStatus {
    PENDING,   // 입금대기 (무통장 입금 선택 후 아직 돈을 안 보낸 상태)
    CONFIRMED, // 결제완료 (카드/간편결제 성공 또는 무통장 입금 확인 완료)
    CANCELED   // 결제취소 (예매 취소 및 환불 완료 상태)
}
