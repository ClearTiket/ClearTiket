package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.seat.SeatResponse;
import com.clearticket.clearticket.model.dto.seat.SeatStatusEvent;
import com.clearticket.clearticket.model.entity.BookingSeat;
import com.clearticket.clearticket.model.entity.Schedule;
import com.clearticket.clearticket.model.entity.Seat;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.clearticket.clearticket.repository.UserRepository;
import com.clearticket.clearticket.repository.BookingSeatsRepository;
import com.clearticket.clearticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByPerformance(Long performanceId) {
        List<Seat> allSeats = seatRepository.findByPerformancePerformanceId(performanceId);
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Long> bookedSeatIds = bookingSeatsRepository.findSeatIdsByCreatedAtAfter(fiveMinutesAgo);

        return allSeats.stream().map(seat -> {
            String status = bookedSeatIds.contains(seat.getSeatId()) ? "BOOKED" : "AVAILABLE";
            return new SeatResponse(seat, status);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void bookSeat(Long seatId, Long userId, Long scheduleId) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        if (bookingSeatsRepository.existsBySeatSeatIdAndCreatedAtAfter(seatId, fiveMinutesAgo)) {
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

        broadcastSeatStatus(scheduleId, seatId, "BOOKED");
    }

    @Transactional
    public void releaseSeat(Long seatId, Long userId, Long scheduleId) {
        bookingSeatsRepository.deleteBySeatSeatIdAndUserUserId(seatId, userId);
        broadcastSeatStatus(scheduleId, seatId, "AVAILABLE");
    }

    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void releaseExpiredBookings() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<Long> expiredSeatIds = bookingSeatsRepository.findExpiredSeatIds(fiveMinutesAgo);
        if (expiredSeatIds.isEmpty()) return;

        bookingSeatsRepository.deleteByCreatedAtBefore(fiveMinutesAgo);

        expiredSeatIds.forEach(seatId ->
                seatRepository.findById(seatId).ifPresent(seat -> {
                    if (seat.getPerformance() == null) return;
                    Long performanceId = seat.getPerformance().getPerformanceId();
                    messagingTemplate.convertAndSend(
                            "/topic/seats/performance/" + performanceId,
                            new SeatStatusEvent(seatId, "AVAILABLE"));
                })
        );
    }

    private void broadcastSeatStatus(Long scheduleId, Long seatId, String status) {
        messagingTemplate.convertAndSend(
                "/topic/seats/" + scheduleId,
                new SeatStatusEvent(seatId, status)
        );
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsBySchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄 정보를 찾을 수 없습니다."));
        Long performanceId = schedule.getPerformance().getPerformanceId();
        return getSeatsByPerformance(performanceId);
    }
}