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

    // 실시간 매진 알림 위젯용: 판매중인 공연들의 잔여석 현황
    @GetMapping("/live-alerts")
    public ResponseEntity<List<com.clearticket.clearticket.model.dto.seat.LiveSeatAlertResponse>> getLiveAlerts(
            @RequestParam(value = "limit", defaultValue = "5") int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 20));
        return ResponseEntity.ok(seatService.getLiveSoldOutAlerts(safeLimit));
    }

    @PostMapping("/book")
    public ResponseEntity<String> bookSeat(
            @RequestParam String sectionName,
            @RequestParam String rowNum,
            @RequestParam Integer seatNum,
            @RequestParam Long userId,
            @RequestParam Long scheduleId) {

        try {
            seatService.bookSeat(sectionName, rowNum, seatNum, userId, scheduleId);

            return ResponseEntity.ok(
                    "좌석이 임시 선점되었습니다! 5분 내로 결제를 완료해 주세요."
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping("/release")
    public ResponseEntity<String> releaseSeat(
            @RequestParam String sectionName,
            @RequestParam String rowNum,
            @RequestParam Integer seatNum,
            @RequestParam Long userId,
            @RequestParam Long scheduleId) {

        seatService.releaseSeat(scheduleId, sectionName, rowNum, seatNum, userId);

        return ResponseEntity.ok("좌석 선점이 해제되었습니다.");
    }
}