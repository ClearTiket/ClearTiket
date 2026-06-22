package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Review {
    // 1. 리뷰 ID (Primary Key)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 3. 공연 ID (외래키 역할)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    private Performance performance;

    // 4. 별점 (예: 1~5점)
    @Column(nullable = true)
    private Integer rating; // null 허용을 위해 Integer 사용

    // 5. 리뷰 내용 (TEXT 타입 매핑)
    @Column(nullable = true, columnDefinition = "TEXT")
    private String content;

    // 6. 작성일시
    @CreationTimestamp
    @Column(nullable = true, updatable = false)
    private LocalDateTime createdAt; // @CreationTimestamp가 실시간으로 시간을 채워줍니다!
    // +컬럼 추가+
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "views", nullable = false)
    @Builder.Default
    private int views = 0;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private String type = "REVIEW";

    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private String status = "Y"; // 기본값 "Y" (노출 상태)



}
