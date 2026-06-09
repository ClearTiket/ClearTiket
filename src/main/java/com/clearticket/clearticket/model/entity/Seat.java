package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    Long seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    Venue venue;

    @Column(length = 20)
    String seatGrade;

    @Column(length = 20)
    String sectionName;

    @Column(length = 10)
    String rowNum;

    @Column(length = 10)
    String seatNum;

    Integer price;
}
