package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.seat.LiveSeatAlertResponse;
import com.clearticket.clearticket.model.dto.seat.SeatResponse;
import com.clearticket.clearticket.model.dto.seat.SeatStatusEvent;
import com.clearticket.clearticket.model.entity.*;
import com.clearticket.clearticket.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatGeneratorService seatGenerator;
    private final BookingSeatsRepository bookingSeatsRepository;
    private final ReservationSeatsRepository reservationSeatsRepository;
    private final PerformanceRepository performanceRepository;
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
                    // 이 좌석을 선점한 회원 ID를 함께 내려줘서, 프론트에서 "내가 이미 선택한 좌석"과
                    // "다른 사람이 선점해서 진짜 막힌 좌석"을 구분할 수 있게 함
                    if (booked.getUser() != null) {
                        seat.setUserId(booked.getUser().getUserId());
                    }
                }
            }
        }

        return allSeats;
    }

    /**
     * 실시간 매진 알림 위젯용 데이터.
     * 판매중(ON_SALE)인 공연들 중, 가장 가까운 회차를 기준으로
     * "임시 선점 + 예매 확정" 좌석 수를 빼서 실제 잔여석을 계산해 내려준다.
     */
    @Transactional(readOnly = true)
    public List<LiveSeatAlertResponse> getLiveSoldOutAlerts(int limit) {
        int totalSeats = seatGenerator.generateSeats().size();
        LocalDate today = LocalDate.now();

        // 후보를 넉넉히 가져와서(요청한 개수의 4배), 다가오는 회차가 없는 공연은 걸러낸다.
        List<Performance> candidates =
                performanceRepository.findAllByStatusIs(PerformanceStatus.ON_SALE, Limit.of(Math.max(limit * 4, 20)));

        List<LiveSeatAlertResponse> result = new ArrayList<>();

        for (Performance performance : candidates) {
            if (result.size() >= limit) break;

            Optional<Schedule> nextSchedule = scheduleRepository
                    .findFirstByPerformance_PerformanceIdAndShowDateGreaterThanEqualOrderByShowDateAscShowTimeAsc(
                            performance.getPerformanceId(), today);

            if (nextSchedule.isEmpty()) continue;

            Schedule schedule = nextSchedule.get();

            long heldSeats = bookingSeatsRepository.countByScheduleScheduleId(schedule.getScheduleId());
            long confirmedSeats = reservationSeatsRepository.countActiveByScheduleId(schedule.getScheduleId());

            int remaining = (int) Math.max(0, totalSeats - heldSeats - confirmedSeats);

            result.add(LiveSeatAlertResponse.builder()
                    .performanceId(performance.getPerformanceId())
                    .scheduleId(schedule.getScheduleId())
                    .title(performance.getTitle())
                    .venueName(performance.getVenue() != null ? performance.getVenue().getName() : "")
                    .posterUrl(performance.getPosterUrl())
                    .totalSeats(totalSeats)
                    .remainingSeats(remaining)
                    .build());
        }

        return result;
    }
}