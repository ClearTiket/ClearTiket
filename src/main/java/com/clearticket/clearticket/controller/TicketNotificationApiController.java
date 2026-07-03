package com.clearticket.clearticket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class TicketNotificationApiController {

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/soldout")
    public void broadcastSoldOut(@RequestParam("performanceId") Long performanceId,
                                 @RequestParam("section") String section) {

        log.info("✅ 실시간 매진 알림 발송 - 공연 ID: {}, 구역: {}", performanceId, section);

        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("performanceId", performanceId);
        messagePayload.put("section", section);
        messagePayload.put("status", "SOLD_OUT");
        messagePayload.put("notice", section + " 좌석이 방금 모두 매진되었습니다. 다른 구역을 선택해 주세요!");

        String destination = "/topic/soldout/" + performanceId;

        messagingTemplate.convertAndSend(destination, (Object) messagePayload);

        log.info("✅ 실시간 매진 알림 브로드캐스팅 완료: {}", destination);
    }
}