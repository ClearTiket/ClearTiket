package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    Long userId;

    @Column(nullable = false, length = 100)
    String email;

    @Column(nullable = false, length = 255)
    String password;

    @Column(nullable = false, length = 50)
    String name;

    @Column(nullable = false, length = 13)
    String phone;

    @Column(nullable = false, length = 255)
    String address;

    @Column(length = 50)
    String preferenceGenre;

    @Column(nullable = false)
    @CreationTimestamp
    LocalDateTime createdAt;
}
