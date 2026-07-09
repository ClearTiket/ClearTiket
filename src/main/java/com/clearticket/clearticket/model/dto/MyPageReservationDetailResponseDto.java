package com.clearticket.clearticket.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class MyPageReservationDetailResponseDto {
    private Long reservationId;
    private String reservationNumber;
    private LocalDateTime reservationDate;   // 예매일시
    private String performanceTitle;
    private String posterImageUrl;
    private String venueName;
    private LocalDateTime showDateTime;      // 관람일시
    private String status;                   // WAITING / CONFIRMED / CANCELED
    private String ticketType;               // 수령방법
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;

    private List<MyPageReservationSeatDto> seats;
    private int ticketPriceSum;   // 좌석 합산 금액
    private int shippingFee;      // 결제수수료/배송비
    private int discountAmount;   // 쿠폰 할인
    private int totalPrice;       // 총 결제금액

    private boolean canCancel;         // 취소 가능 여부 (상태 + 관람일 기준)
    private String cancelDeadlineText; // 취소 가능 기한 안내 문구
    private int cancelFeeRatePercent;  // 현재 시점 기준 취소 수수료율(%)
    private int cancelFeeAmount;       // 예상 취소 수수료
    private int refundAmount;          // 예상 환불액 (totalPrice - cancelFeeAmount)
}
