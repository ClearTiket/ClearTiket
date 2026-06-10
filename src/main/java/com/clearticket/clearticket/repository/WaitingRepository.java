package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitingRepository extends JpaRepository<Venue,Long> {

}
