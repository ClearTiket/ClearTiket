package com.clearticket.clearticket.controller;

//import com.clearticket.clearticket.model.entity.Reservation;
import com.clearticket.clearticket.model.dto.ReservationRequestDto;
import com.clearticket.clearticket.model.dto.ReservationResponseDto;
import com.clearticket.clearticket.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationApiController {

    private final ReservationService reservationService;

    /**
     * [결제 완료] 예매 및 결제 생성
     * POST http://localhost:8080/api/reservations
     */
    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(
            @RequestBody ReservationRequestDto requestDto) {
        ReservationResponseDto response = reservationService.createReservation(requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * [상세 조회] 예매 상세 조회
     * GET http://localhost:8080/api/reservations/{reservation_id}
     */
    @GetMapping("/{reservation_id}")
    public ResponseEntity<ReservationResponseDto> getReservationById(
            @PathVariable("reservation_id") Long reservationId) {
        ReservationResponseDto response = reservationService.getReservationById(reservationId);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 예약 내역 목록 조회
     * @return 조회된 예약 정보 응답 DTO 객체들이 담긴 리스트
     */
    @GetMapping("/all")
    public ResponseEntity<List<ReservationResponseDto>> getAllReservations() {
        List<ReservationResponseDto> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    /**
     * [예매 취소] 예매 취소 실행
     * POST http://localhost:8080/api/reservations/{reservation_id}/cancel
     */
    @PostMapping("/{reservation_id}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable("reservation_id") Long reservationId) {
        reservationService.cancelReservationById(reservationId);
        return ResponseEntity.ok().build();
    }
}