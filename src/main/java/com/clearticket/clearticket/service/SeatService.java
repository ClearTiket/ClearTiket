package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.seat.SeatResponse;
import com.clearticket.clearticket.model.entity.BookingSeat;
import com.clearticket.clearticket.model.entity.Schedule;
import com.clearticket.clearticket.model.entity.Seat;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.clearticket.clearticket.repository.UserRepository;
import com.clearticket.clearticket.repository.BookingSeatsRepository;
import com.clearticket.clearticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // 💡 이제 UserRepository까지 포함해서 싱싱한 생성자를 다시 만듭니다!
public class SeatService {

    private final SeatRepository seatRepository;
    private final BookingSeatsRepository bookingSeatsRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;

    // 1. 특정 공연의 모든 좌석 상태를 조회하는 로직
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByPerformance(Long performanceId) {
        List<Seat> allSeats = seatRepository.findByPerformancePerformanceId(performanceId);

        // 5분 이내에 예약된 모든 좌석 ID를 미리 한 번에 조회합니다
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Long> bookedSeatIds = bookingSeatsRepository.findSeatIdsByCreatedAtAfter(fiveMinutesAgo);

        return allSeats.stream().map(seat -> {
            // 5분이 지난 데이터는 선점 안 된 걸로 쳐서 다른 사람에게 "AVAILABLE(예매 가능)"으로 보여줍니다!
            String status = bookedSeatIds.contains(seat.getSeatId()) ? "BOOKED" : "AVAILABLE";
            return new SeatResponse(seat, status);
        }).collect(Collectors.toList());
    }
    // 2. 좌석을 임시 선점(찜)하는 로직
    @Transactional
    public void bookSeat(Long seatId, Long userId) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        // 이미 다른 사람이 찜했는지 검사
        if (bookingSeatsRepository.existsBySeatSeatIdAndCreatedAtAfter(seatId, fiveMinutesAgo)) {
            throw new IllegalStateException("이미 선점된 좌석입니다! 다른 좌석을 선택해 주세요.");
        }

        // 좌석 정보 가져오기
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        // 엔티티 구조에 맞춰 진짜 User 객체를 DB에서 안전하게 찾아옵니다!
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 엔티티 설계 양식(user, seat)에 맞춰 싹 바인딩해 줍니다.
        BookingSeat bookingSeat = BookingSeat.builder()
                .seat(seat)
                .user(user)
                .build();

        bookingSeatsRepository.save(bookingSeat);
    }
    // 스케줄 정보
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsBySchedule(Long scheduleId) {
        // scheduleId로 공연 정보를 조회
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄 정보를 찾을 수 없습니다."));

        // 해당 스케줄에 연결된 공연(Performance)의 ID 추출
        Long performanceId = schedule.getPerformance().getPerformanceId();

        // 3. 기존 메서드를 재사용하여 좌석 정보 조회
        return getSeatsByPerformance(performanceId);
    }


}