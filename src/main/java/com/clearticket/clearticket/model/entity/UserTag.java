package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_tags")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserTag {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Foreign Key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    // Foreign Key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    Tag tag;

    @Column(nullable = false)
    double aiScore;
}
