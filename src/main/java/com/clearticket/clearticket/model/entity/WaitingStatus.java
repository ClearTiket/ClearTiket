package com.clearticket.clearticket.model.entity;

public enum WaitingStatus {
    WAITING,   // 예매 대기 신청 후 순번 기다리는 중
    NOTIFIED,  // 대기 순번이 도래하여 결제 가능하다고 안내된 상태
    COMPLETED, // 결제까지 정상적으로 완료되어 예매 확정된 상태
    EXPIRED    // 대기 시간이 만료되었거나 사용자가 대기를 취소한 상태
}
