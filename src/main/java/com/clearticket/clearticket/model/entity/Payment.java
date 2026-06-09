package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Payment {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long payment_id;

    // Foriegn key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    Reservation reservation;

    @Column(nullable = false, length = 50)
    PaymentMethod paymentMethod;

    @Column(length = 50)
    String bankName;

    @Column(nullable = false)
    int amount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    PaymentStatus status;

    @CreationTimestamp
    LocalDateTime paymentDate;
}

enum PaymentMethod {
    CARD,
    BANK_TRANSFER,
    EASY_PAYMENT
}

enum PaymentStatus {
    PENDING,
    CONFIRMED,
    CANCELED
}