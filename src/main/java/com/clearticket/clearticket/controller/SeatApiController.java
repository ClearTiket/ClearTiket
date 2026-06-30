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
public class SeatApiController {

    private final SeatService seatService;

    @GetMapping
    public ResponseEntity<?> getSeats(@RequestParam("scheduleId") Long scheduleId) {
        try {
            List<SeatResponse> seats = seatService.getSeatsBySchedule(scheduleId);
            return ResponseEntity.ok(seats);
        } catch (IllegalArgumentException e) {
            // scheduleId가 존재하지 않을 때 500 대신 명확한 404를 내려줍니다.
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/book")
    public ResponseEntity<String> bookSeat(
            @RequestParam Long seatId,
            @RequestParam Long userId,
            @RequestParam Long scheduleId) {
        try {
            seatService.bookSeat(seatId, userId, scheduleId);
            return ResponseEntity.ok("좌석이 임시 선점되었습니다! 5분 내로 결제를 완료해 주세요.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/release")
    public ResponseEntity<String> releaseSeat(
            @RequestParam Long seatId,
            @RequestParam Long userId,
            @RequestParam Long scheduleId) {
        seatService.releaseSeat(seatId, userId, scheduleId);
        return ResponseEntity.ok("좌석 선점이 해제되었습니다.");
    }
}