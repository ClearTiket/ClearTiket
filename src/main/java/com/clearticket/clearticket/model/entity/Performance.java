package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "performances")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Performance {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long performanceId;

    // Foreign Key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    Venue venue;

    @Column(nullable = false, length = 50)
    String kopisId;

    @Column(nullable = false, length = 200)
    String title;

    @Column(nullable = false, length = 50)
    String genre;

    @Column(nullable = false, length = 50)
    String region;

    @Column(nullable = false)
    LocalDate startDate;

    @Column(nullable = false)
    LocalDate endDate;

    @Column(nullable = false, length = 255)
    String posterUrl;

    @Column(length = 1000)
    String castings;

    @Column(nullable = false)
    int runtime;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    PerformanceStatus status;

    @Column(length = 255)
    String introImageUrl;

    @Column(length = 3000)
    String extractedText;

    @CreationTimestamp
    LocalDateTime createdAt;
}

enum PerformanceStatus {
    PREPARING,
    ON_SALE,
    CLOSED
}