package com.clearticket.clearticket.model.dto.performance;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VenueLayoutResponse {
    private Long venue_id;
    private int rows;
    private int cols;
    private List<SeatGradeInfo> gradeInfos;
}
