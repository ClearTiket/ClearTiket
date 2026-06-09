package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_seats") // 대문자가 섞여있어도 DB에는 스네이크 케이스로 매핑됩니다.
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_seat_id")
    private Long bookingSeatId; // DB의 booking_seat_id와 매핑

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat; // DB의 seat_id와 매핑

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule; // DB의 schedule_id와 매핑 (공연 일정 ID)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BookingStatus status; // DB의 status와 매핑 (예: "SELECTED", "PENDING")

}

enum BookingStatus {
    SELECTED,
    PENDING
}