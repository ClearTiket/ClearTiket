package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.seat.SeatResponse;
import com.clearticket.clearticket.model.dto.seat.SeatStatusEvent;
import com.clearticket.clearticket.model.entity.BookingSeat;
import com.clearticket.clearticket.model.entity.Schedule;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.repository.ScheduleRepository;
import com.clearticket.clearticket.repository.UserRepository;
import com.clearticket.clearticket.repository.BookingSeatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatGeneratorService seatGenerator;
    private final BookingSeatsRepository bookingSeatsRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByPerformance(Long performanceId) {
        return seatGenerator.generateSeats();
    }

    @Transactional
    public void bookSeat(
            String sectionName,
            String rowNum,
            Integer seatNum,
            Long userId,
            Long scheduleId) {

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        boolean exists = bookingSeatsRepository
                .existsByScheduleScheduleIdAndSectionNameAndRowNumAndSeatNumAndCreatedAtAfter(
                        scheduleId, sectionName, rowNum, seatNum, fiveMinutesAgo);

        if (exists) {
            throw new IllegalStateException("이미 선점된 좌석입니다! 다른 좌석을 선택해 주세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        BookingSeat bookingSeat = BookingSeat.builder()
                .user(user)
                .schedule(schedule)
                .sectionName(sectionName)
                .rowNum(rowNum)
                .seatNum(seatNum)
                .status(com.clearticket.clearticket.model.entity.BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        bookingSeatsRepository.save(bookingSeat);

        broadcastSeatStatus(scheduleId, sectionName, rowNum, seatNum, "BOOKED");
    }

    @Transactional
    public void releaseSeat(
            Long scheduleId,
            String sectionName,
            String rowNum,
            Integer seatNum,
            Long userId) {

        bookingSeatsRepository
                .deleteByScheduleScheduleIdAndSectionNameAndRowNumAndSeatNumAndUserUserId(
                        scheduleId, sectionName, rowNum, seatNum, userId);

        broadcastSeatStatus(scheduleId, sectionName, rowNum, seatNum, "AVAILABLE");
    }

    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void releaseExpiredBookings() {

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        List<BookingSeat> expiredSeats =
                bookingSeatsRepository.findByCreatedAtBefore(fiveMinutesAgo);

        if (expiredSeats.isEmpty()) return;

        for (BookingSeat seat : expiredSeats) {

            Schedule schedule = seat.getSchedule();

            bookingSeatsRepository.delete(seat);

            broadcastSeatStatus(
                    schedule.getScheduleId(),
                    seat.getSectionName(),
                    seat.getRowNum(),
                    seat.getSeatNum(),
                    "AVAILABLE"
            );
        }
    }

    private void broadcastSeatStatus(
            Long scheduleId,
            String sectionName,
            String rowNum,
            Integer seatNum,
            String status) {

        messagingTemplate.convertAndSend(
                "/topic/seats/" + scheduleId,
                new SeatStatusEvent(sectionName, rowNum, seatNum, status)
        );
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsBySchedule(Long scheduleId) {

        scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄 정보를 찾을 수 없습니다."));

        List<SeatResponse> allSeats = seatGenerator.generateSeats();

        List<BookingSeat> bookedSeats = bookingSeatsRepository.findByScheduleScheduleId(scheduleId);

        for (SeatResponse seat : allSeats) {
            for (BookingSeat booked : bookedSeats) {
                if (seat.getSectionName().equals(booked.getSectionName()) &&
                        seat.getRowNum().equals(booked.getRowNum()) &&
                        seat.getSeatNum().equals(booked.getSeatNum())) {

                    if (booked.getStatus() != null) {
                        seat.setStatus(booked.getStatus().name());
                    } else {
                        seat.setStatus("PENDING");
                    }
                }
            }
        }

        return allSeats;
    }
}