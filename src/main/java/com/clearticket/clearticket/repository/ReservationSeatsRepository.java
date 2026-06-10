package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationSeatsRepository extends JpaRepository<ReservationSeat, Long> {

}