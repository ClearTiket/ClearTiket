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
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * [결제 완료] 예매 및 결제 생성
     * POST http://localhost:8080/api/reservations
     */
//    @PostMapping
//    public ResponseEntity<Reservation> createReservation(@RequestBody Reservation reservation) {
//        Reservation savedReservation = reservationService.createReservation(reservation);
//        return ResponseEntity.ok(savedReservation);
//    }
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
//    @GetMapping("/{reservation_id}")
//    public ResponseEntity<Reservation> getReservationById(@PathVariable("reservation_id") Long reservationId) {
//        Reservation reservation = reservationService.getReservationById(reservationId);
//        return ResponseEntity.ok(reservation);
//    }
    @GetMapping("/{reservation_id}")
    public ResponseEntity<ReservationResponseDto> getReservationById(
            @PathVariable("reservation_id") Long reservationId) {
        ReservationResponseDto response = reservationService.getReservationById(reservationId);
        return ResponseEntity.ok(response);
    }

    /**
     * [마이페이지] 예매 내역 리스트 조회
     * 기존: 클래스 레벨 @RequestMapping("/api/reservations") + 메서드 "/api/users/me/reservations"
     *       => 실제 매핑 경로가 "/api/reservations/api/users/me/reservations"가 되어버리는 버그
     */
//    @GetMapping("/api/users/me/reservations")
//    public ResponseEntity<List<Reservation>> getAllReservations() {
//        List<Reservation> reservations = reservationService.getAllReservations();
//        return ResponseEntity.ok(reservations);
//    }
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
        // 내부 로직은 우리가 만든 최신 트렌드 '상태 변경(Soft Delete)' 메서드 호출!
//        reservationService.deleteReservationById(reservationId);
        reservationService.cancelReservationById(reservationId);
        return ResponseEntity.ok().build();
    }
}