package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.seat.SeatResponse;
import com.clearticket.clearticket.model.entity.BookingSeat;
import com.clearticket.clearticket.model.entity.Seat;
import com.clearticket.clearticket.repository.BookingSeatsRepository;
import com.clearticket.clearticket.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // 리포지토리 일꾼들 자동 소환!
public class SeatService {

    private final SeatRepository seatRepository;
    private final BookingSeatsRepository bookingSeatsRepository;

    // 1. 특정 공연의 모든 좌석 상태를 조회하는 로직
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByPerformance(Long performanceId) {
        // 우선 해당 공연의 전체 좌석을 다 긁어옵니다.
        List<Seat> allSeats = seatRepository.findByPerformancePerformanceId(performanceId);

        // 각 좌석이 현재 선점(Booking)된 상태인지 체크해서 포장지(DTO)에 예쁘게 담아줍니다.
        return allSeats.stream().map(seat -> {
            boolean isBooked = bookingSeatsRepository.existsBySeatSeatId(seat.getSeatId());
            String status = isBooked ? "BOOKED" : "AVAILABLE";
            return new SeatResponse(seat, status);
        }).collect(Collectors.toList());
    }

    // 2. 좌석을 임시 선점(찜)하는 로직
    @Transactional
    public void bookSeat(Long seatId, Long userId) {
        // 이미 다른 사람이 찜했는지 검사
        if (bookingSeatsRepository.existsBySeatSeatId(seatId)) {
            throw new IllegalStateException("이미 선점된 좌석입니다! 다른 좌석을 선택해 주세요.");
        }

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        // 선점 테이블 창고에 데이터 쌓기 (5분간 임시 선점 가정)
        BookingSeat bookingSeat = BookingSeat.builder()
                .seat(seat)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        bookingSeatsRepository.save(bookingSeat);
    }
}