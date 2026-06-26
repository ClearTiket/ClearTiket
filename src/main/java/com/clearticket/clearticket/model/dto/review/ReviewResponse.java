package com.clearticket.clearticket.model.dto.review;

import com.clearticket.clearticket.model.entity.Review;
import java.util.List;

public record ReviewResponse (
    List<Review> reviews,
    long totalCount,
    double averageRating
) {}