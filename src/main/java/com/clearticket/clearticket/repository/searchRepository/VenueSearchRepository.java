package com.clearticket.clearticket.repository.searchRepository;

import com.clearticket.clearticket.model.document.VenueDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VenueSearchRepository extends ElasticsearchRepository<VenueDocument, Long> {

}
