package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    // 외래키(FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_coupon_id")
    private Coupon usedCoupon;

    // 일반 컬럼들
    @Column(nullable = false)
    private String reservationNumber; // 예매번호

    @Column(nullable = false)
    private int totalPrice; // 총 결제 금액

    @Column(nullable = false, length = 50)
    private String ticketType; // 티켓 수령 방식 (지류/모바일 등)

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status; // 예약 상태 (WAITING, CONFIRMED, CANCELED)

    @Column(nullable = false)
    private int shippingFee; // 배송료

    @Column(length = 50)
    private String recipientName; // 수령인 이름

    @Column(length = 13)
    private String recipientPhone; // 수령인 연락처

    @Column(length = 255)
    private String shippingAddress; // 배송 주소

    // 예매 일시
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void changeStatus(ReservationStatus newStatus) {
        this.status = newStatus;
    }
}

