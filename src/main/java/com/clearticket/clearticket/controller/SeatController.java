package com.clearticket.clearticket.controller;

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

    // GET /api/seats?performanceId=1&scheduleId=501
    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeats(
            @RequestParam("performanceId") Long performanceId,
            @RequestParam("scheduleId") Long scheduleId) {
        List<SeatResponse> seats = seatService.getSeatsByPerformance(performanceId, scheduleId);
        return ResponseEntity.ok(seats);
    }

    // POST /api/seats/book?seatId=1&scheduleId=501&userId=1
    @PostMapping("/book")
    public ResponseEntity<String> bookSeat(
            @RequestParam Long seatId,
            @RequestParam Long scheduleId,
            @RequestParam Long userId) {
        try {
            seatService.bookSeat(seatId, scheduleId, userId);
            return ResponseEntity.ok("좌석이 임시 선점되었습니다! 5분 내로 결제를 완료해 주세요.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}