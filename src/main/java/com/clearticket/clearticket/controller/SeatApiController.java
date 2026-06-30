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
    public ResponseEntity<List<SeatResponse>> getSeats(@RequestParam("scheduleId") Long scheduleId) {
        List<SeatResponse> seats = seatService.getSeatsBySchedule(scheduleId);
        return ResponseEntity.ok(seats); // 200 OK 상태코드와 함께 좌석 포장지 묶음을 반환!
    }

    @PostMapping("/book")
    public ResponseEntity<String> bookSeat(@RequestParam Long seatId, @RequestParam Long userId) {
        try {
            seatService.bookSeat(seatId, userId);
            return ResponseEntity.ok("좌석이 임시 선점되었습니다! 5분 내로 결제를 완료해 주세요.");
        } catch (IllegalStateException e) {
            // 이미 선점된 좌석일 때 예외 처리
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // 좌석이나 회원이 없을 때 예외 처리
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}