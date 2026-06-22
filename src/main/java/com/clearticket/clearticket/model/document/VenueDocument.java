package com.clearticket.clearticket.model.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

@Getter
@Setter
@Document(indexName = "venues")
public class VenueDocument {

    @Id
    Long venueId;

    @Field(type = FieldType.Text, analyzer = "nori_korean_analyzer")
    String name;

    @Field(type = FieldType.Text)
    String address;

    @Field(type = FieldType.Keyword)
    String region;

    GeoPoint location;

    @Field(type = FieldType.Keyword)
    String telnum;

    @Field(type = FieldType.Keyword)
    String relateurl;

    @Field(type = FieldType.Integer)
    int capacity;
}
