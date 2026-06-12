package com.clearticket.clearticket.model.dto.performance;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewItem {
    private Long reviewId;
    private String nickname;
    private String content;
    private int rating;
    private String createdAt;
    
}
