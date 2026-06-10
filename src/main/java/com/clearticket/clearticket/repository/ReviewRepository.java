package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReservationSeat, Long> {

}
