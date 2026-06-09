package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tags")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Tag {

    // Primary Key
    @Id
    Integer tag_id;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    TagCategory tagCategory;

    @Column(nullable = false, length = 20)
    String tagName;

    @Column(nullable = false, length = 20)
    String displayName;
}

enum TagCategory {
    GENRE,
    VIBE,
    WITH
}
