package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.seat.SeatResponse;
import com.clearticket.clearticket.model.entity.BookingSeat;
import com.clearticket.clearticket.model.entity.Schedule;
import com.clearticket.clearticket.model.entity.Seat;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.repository.BookingSeatsRepository;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.clearticket.clearticket.repository.SeatRepository;
import com.clearticket.clearticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final BookingSeatsRepository bookingSeatsRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;

    // 1. 특정 공연 + 특정 회차의 모든 좌석 상태 조회
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByPerformance(Long performanceId, Long scheduleId) {
        List<Seat> allSeats = seatRepository.findByPerformancePerformanceId(performanceId);

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        return allSeats.stream().map(seat -> {
            boolean isBooked = bookingSeatsRepository
                    .existsBySeatSeatIdAndScheduleScheduleIdAndCreatedAtAfter(
                            seat.getSeatId(), scheduleId, fiveMinutesAgo);
            String status = isBooked ? "BOOKED" : "AVAILABLE";
            return new SeatResponse(seat, status);
        }).collect(Collectors.toList());
    }

    // 2. 좌석 임시 선점
    @Transactional
    public void bookSeat(Long seatId, Long scheduleId, Long userId) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        if (bookingSeatsRepository.existsBySeatSeatIdAndScheduleScheduleIdAndCreatedAtAfter(
                seatId, scheduleId, fiveMinutesAgo)) {
            throw new IllegalStateException("이미 선점된 좌석입니다! 다른 좌석을 선택해 주세요.");
        }

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        BookingSeat bookingSeat = BookingSeat.builder()
                .seat(seat)
                .user(user)
                .schedule(schedule)
                .build();

        bookingSeatsRepository.save(bookingSeat);
    }
}