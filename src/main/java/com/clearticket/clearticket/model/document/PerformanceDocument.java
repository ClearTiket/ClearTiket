package com.clearticket.clearticket.model.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Document(indexName = "performances")
public class PerformanceDocument {

    @Id
    Long performanceId;

    @Field(type = FieldType.Text, analyzer = "nori_korean_analyzer")
    String title;

    @Field(type = FieldType.Keyword)
    String genre;

    @Field(type = FieldType.Keyword)
    String region;

    @Field(type = FieldType.Keyword)
    String status;

    @Field(name = "start_date", type = FieldType.Date)
    String startDate;

    @Field(name = "end_date", type = FieldType.Date)
    String endDate;

    @Field(type = FieldType.Text, analyzer = "nori_korean_analyzer")
    String castings;

    @Field(name = "poster_url", type = FieldType.Text)
    String posterUrl;

    @Field(name = "extracted_text", type = FieldType.Text, analyzer = "nori_korean_analyzer")
    String extractedText;

    @Field(name = "venue_name", type = FieldType.Text, analyzer = "nori_korean_analyzer")
    String venueName;
}
