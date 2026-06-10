package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitings")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Waiting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long waitingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="schedule_id")
    Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private WaitingStatus status; // 대기상태 (WAITING, NOTIFIED, COMPLETED, EXPIRED)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

enum WaitingStatus {
    WAITING,
    NOTIFIED,
    COMPLETED,
    EXPIRED
}