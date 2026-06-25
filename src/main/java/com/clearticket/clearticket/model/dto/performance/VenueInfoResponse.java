package com.clearticket.clearticket.model.dto.performance;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VenueInfoResponse {
    private String title;
    private String venueName;
    private int totalSeats;
    private String address;
    private String telnum;
    private String relateurl;

}
