package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.seat.BookSeatResponse;
import com.clearticket.clearticket.model.dto.seat.SeatResponse;
import com.clearticket.clearticket.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    // 💡 1. 좌석 목록 조회 API (GET http://localhost:8080/api/seats?performanceId=1)
    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeats(@RequestParam("performanceId") Long performanceId) {
        List<SeatResponse> seats = seatService.getSeatsByPerformance(performanceId);
        return ResponseEntity.ok(seats); // 200 OK 상태코드와 함께 좌석 포장지 묶음을 반환!
    }

    // 💡 2. 좌석 임시 선점 API (POST http://localhost:8080/api/seats/book)
    @PostMapping("/book")
    public ResponseEntity<String> bookSeat(@RequestBody BookSeatResponse request) {
        try {
            seatService.bookSeat(request.getSeatId(), request.getUserId());
            return ResponseEntity.ok("좌석 선점에 성공했습니다. 5분 내로 결제를 완료해 주세요!");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 이미 선점된 경우 400 에러와 메시지 반환
        }
    }
}