package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
