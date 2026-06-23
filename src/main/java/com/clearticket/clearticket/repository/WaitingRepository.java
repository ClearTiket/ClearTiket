package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Schedule;
import com.clearticket.clearticket.model.entity.Waiting;
import com.clearticket.clearticket.model.entity.WaitingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {

    /**
     * 회원이 신청한 모든 예매 대기 내역 조회
     * @param userId 조회할 회원 ID
     * @return 해당 회원의 예매 대기 목록 리스트
     */
    List<Waiting> findByUserUserId(Long userId);

    /**
     * 회원의 앞 대기 인원 계산용
     * @param schedule 조회할 공연 일정 객체
     * @param createdAt 기준ㄹ이 되는 내 대기 선청일시
     * @param waitingStatus 조회할 대기 상태(주로 WAITING)
     * @return 회원 앞 대기 인원수
     */
    int countByScheduleAndCreatedAtBeforeAndStatus(Schedule schedule, LocalDateTime createdAt, WaitingStatus waitingStatus);
}
