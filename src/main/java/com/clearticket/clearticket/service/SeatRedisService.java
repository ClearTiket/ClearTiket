package com.clearticket.clearticket.service; // 혜인님 프로젝트 패키지 구조에 맞게 자동 완성 처리하세요!

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SeatRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis에 저장할 Key의 접두사 (티켓팅 매진 상태 관리용)
    private static final String SOLD_OUT_KEY_PREFIX = "soldout:performance:";

    /**
     * 특정 공연의 특정 구역을 매진 상태로 등록하는 메서드
     * @param performanceId 공연 ID
     * @param section 구역 이름 (예: "5구역", "A구역")
     */
    public void markSectionAsSoldOut(Long performanceId, String section) {
        String key = SOLD_OUT_KEY_PREFIX + performanceId;

        // Redis의 Set 자료구조를 사용하여 매진된 구역들을 한곳에 모아 관리
        redisTemplate.opsForSet().add(key, section);

        // 티켓팅이 무한히 진행되진 않으니 데이터가 평생 남아있지 않게 2시간 뒤 자동 삭제되도록 설정
        redisTemplate.expire(key, 2, TimeUnit.HOURS);
    }

    /**
     * 특정 공연의 특정 구역이 매진되었는지 확인하는 메서드
     */
    public boolean isSectionSoldOut(Long performanceId, String section) {
        String key = SOLD_OUT_KEY_PREFIX + performanceId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, section));
    }
}