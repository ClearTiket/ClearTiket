package com.clearticket.clearticket.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LemonSqueezyWebhookDto {

    @JsonProperty("event_name")
    private String eventName;

    private Meta meta;


    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        @JsonProperty("custom_data")
        private CustomData customData;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomData {
        @JsonProperty("reservation_id")
        private Long reservationId;
    }
}