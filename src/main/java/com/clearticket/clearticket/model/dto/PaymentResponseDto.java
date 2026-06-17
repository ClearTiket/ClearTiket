package com.clearticket.clearticket.model.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long paymentId;
    private Long reservationId;
    private String performanceName;
    private String paymentMethod;
    private String bankName;
    private int amount;
    private String status; // 결제 상태 (PENDING, CONFIRMED, CANCELED)
    private LocalDateTime paymentDate;
}