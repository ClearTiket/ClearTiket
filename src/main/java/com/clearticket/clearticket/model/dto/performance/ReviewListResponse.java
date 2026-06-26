package com.clearticket.clearticket.model.dto.performance;

import lombok.*;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReviewListResponse {
    private double averageRating;
    private int totalReviewsCount;
    private List<ReviewItem> reviews;
}
