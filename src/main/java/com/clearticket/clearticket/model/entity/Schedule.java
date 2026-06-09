package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    // 외래키(FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    // 일반 컬럼들
    @Column(nullable = false)
    private LocalDate showDate; // 공연 날짜 (YYYY-MM-DD)

    @Column(nullable = false)
    private LocalTime showTime; // 공연 시간 (HH:mm:ss)

    @Column(nullable = false)
    private int roundNumber; // 회차 번호 (예: 1회차, 2회차)

    // 데이터 등록 일시
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}