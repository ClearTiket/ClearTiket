package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rankings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ranking {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer rankingId;

    @Column(nullable = false)
    int ranking;

    @Column(nullable = false, length = 10)
    String period;

    @Column(nullable = false, length = 10)
    String genre;

    @Column(nullable = false, length = 10)
    String kopisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    Performance performance;
}
