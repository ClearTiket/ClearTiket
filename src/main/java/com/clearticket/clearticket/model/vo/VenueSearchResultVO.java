package com.clearticket.clearticket.model.vo;

import com.clearticket.clearticket.model.document.VenueDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class VenueSearchResultVO {
    List<VenueDocument> venueDocumentList;
    int totalPages;
}
