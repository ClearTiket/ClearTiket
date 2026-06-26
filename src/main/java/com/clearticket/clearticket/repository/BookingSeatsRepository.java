package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

//
@Repository
public interface BookingSeatsRepository extends JpaRepository<BookingSeat, Long> {
    // seat_id에 데이터가 존재하는가?
    boolean existsBySeatSeatId(Long seatId);
    // 특정 좌석이 "지정한 시간 이후"에 선점된 적이 있는지 체크하는 메서드!
    // AndCreatedAtAfter jpa 메서드 이름 쿼리 생성 규칙!
    boolean existsBySeatSeatIdAndCreatedAtAfter(Long seatId, LocalDateTime dateTime);

}