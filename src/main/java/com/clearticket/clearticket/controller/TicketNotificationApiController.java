package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TicketNotificationApiController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ReservationService reservationService;

    /**
     * 새로 추가된 데이터 기반 조회 API
     * 사용자가 날짜/회차 버튼을 클릭했을 때 실제 매진된 구역 데이터가 있는지 Redis/DB에서 동적 조회
     */
    @GetMapping("/schedules/{scheduleId}/soldout-check")
    public ResponseEntity<Map<String, Object>> getScheduleSoldOutStatus(@PathVariable("scheduleId") Long scheduleId) {
        log.info("🔍 [매진 데이터 동적 조회] 요청된 스케줄 ID: {}", scheduleId);

        Map<String, Object> status = reservationService.checkSectionSoldOutStatus(scheduleId);

        return ResponseEntity.ok(status);
    }

    /**
     * 기존 매진 알림 브로드캐스팅 API (기존 기능 유지)
     */
    @PostMapping("/notifications/soldout")
    public void broadcastSoldOut(@RequestParam("performanceId") Long performanceId,
                                 @RequestParam("section") String section) {

        log.info("실시간 매진 알림 발송 - 공연 ID: {}, 구역: {}", performanceId, section);

        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("performanceId", performanceId);
        messagePayload.put("section", section);
        messagePayload.put("status", "SOLD_OUT");
        messagePayload.put("notice", section + " 좌석이 방금 모두 매진되었습니다. 다른 구역을 선택해 주세요!");

        String destination = "/topic/soldout/" + performanceId;

        messagingTemplate.convertAndSend(destination, (Object) messagePayload);

        log.info("실시간 매진 알림 브로드캐스팅 완료: {}", destination);
    }
}