package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "venues")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Venue {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long venueId;

    @Column(nullable = false, length = 50, unique = true)
    String kopisId;

    @Column(nullable = false, length = 100)
    String name;

    @Column(length = 255)
    String address;

    @Column(nullable = false)
    double lat;

    @Column(nullable = false)
    double lon;

    @Column(length = 13)
    String telnum;

    @Column(length = 255)
    String relateurl;

    @Column(nullable = false)
    int capacity;

    @CreationTimestamp
    LocalDateTime created_at;
}
