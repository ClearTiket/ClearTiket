package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingSeatsRepository extends JpaRepository<BookingSeat, Long> {

}