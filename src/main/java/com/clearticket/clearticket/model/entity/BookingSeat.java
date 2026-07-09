package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_seats") // 대문자가 섞여있어도 DB에는 스네이크 케이스로 매핑됩니다.
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingSeatId; // DB의 booking_seat_id와 매핑

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 1)
    private String sectionName; // A, B, C

    @Column(nullable = false, length = 1)
    private String rowNum;        // A ~ J

    @Column(nullable = false)
    private Integer seatNum;      // 1 ~ 10

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule; // DB의 schedule_id와 매핑 (공연 일정 ID)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookingStatus status; // DB의 status와 매핑 (예: "SELECTED", "PENDING")

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

}