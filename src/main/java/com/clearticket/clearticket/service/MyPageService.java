package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.GenreRatioDto;
import com.clearticket.clearticket.model.dto.MyPageReservationResponseDto;
import com.clearticket.clearticket.model.dto.MyPageStatisticsResponseDto;
import com.clearticket.clearticket.model.dto.MyPageWaitingResponseDto;
import com.clearticket.clearticket.model.entity.*;
import com.clearticket.clearticket.repository.ReservationRepository;
import com.clearticket.clearticket.repository.SeatRepository;
import com.clearticket.clearticket.repository.WaitingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MyPageService {
    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;
    private final SeatRepository seatRepository;

    /**
     * 사용자 통계 및 취향분석 조회
     * @param userId 로그인한 사용자의 고유 ID
     * @return 총 관람시간, 총 관람편수, 장르비율 리스트, 다음 공연 디데이가 포함된 통계 DTO
     */
    @Transactional(readOnly = true)
    public MyPageStatisticsResponseDto getUserStatistics(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserIdAndStatus(userId, ReservationStatus.CONFIRMED);

        int totalWatchingTime = 0;
        int totalCount = reservations.size();

        Map<String, Integer> genreCounts = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closestFutureShow = null;

        for (Reservation reservation : reservations) {
            Schedule schedule = reservation.getSchedule();
            if (schedule == null) {
                continue;
            }

            Performance performance = schedule.getPerformance();
            if (performance == null) {
                continue;
            }

            // 총 관람 시간 누적
            totalWatchingTime += performance.getRuntime();

            // 장르 카운트 누적
            String genre = performance.getGenre();
            if (genre != null) {
                genreCounts.put(genre, genreCounts.getOrDefault(genre, 0) + 1);
            }

            // 가장 가까운 미래의 공연 시간 찾기
            if (schedule.getShowDate() != null && schedule.getShowTime() != null) {
                LocalDateTime showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
                if (showDateTime.isAfter(now)) {
                    if (closestFutureShow == null || showDateTime.isBefore(closestFutureShow)) {
                        closestFutureShow = showDateTime;
                    }
                }
            }
        }

        // 장르 비율 계산 및 DTO 리스트 추가
        List<GenreRatioDto> genreRatios = new ArrayList<>();
        if (totalCount > 0) {
            for (Map.Entry<String, Integer> entry : genreCounts.entrySet()) {
                double percentage = Math.round(((double) entry.getValue() / totalCount) * 1000) / 10.0;
                genreRatios.add(new GenreRatioDto(entry.getKey(), percentage));
            }
            // 퍼센트 높은 순 정렬
            genreRatios.sort((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));
        }

        // 디데이 문자열 계산
        String nextPerformanceDDay = "관람 예정 공연 없음";
        if (closestFutureShow != null) {
            long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), closestFutureShow.toLocalDate());
            if (daysBetween == 0) {
                nextPerformanceDDay = "두근두근, 바로 오늘 클리어! D-Day";
            } else {
                nextPerformanceDDay = "두근두근, 다음 클리어까지 D-" + daysBetween;
            }
        }

        return new MyPageStatisticsResponseDto(totalWatchingTime, totalCount, genreRatios, nextPerformanceDDay);
    }


    /**
     * 내 예매내역 전체 리스트 조회
     * @param userId 로그인한 사용자의 고유 ID
     * @return 마이페이지용 예매내역 응답 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyPageReservationResponseDto> getMyReservations(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserId(userId);
        List<MyPageReservationResponseDto> result = new ArrayList<>();

        for (Reservation reservation : reservations) {
            result.add(toMyPageResponseDto(reservation));
        }

        return result;
    }


    /**
     * 내 예매내역 상세 정보 DTO 변환
     * @param reservation 데이터베이스에서 꺼내온 사용자의 예매내역 원본 데이터
     * @return 마이페이지용 예매내역 응답 DTO
     */
    private MyPageReservationResponseDto toMyPageResponseDto(Reservation reservation) {
        Schedule schedule = reservation.getSchedule();
        Performance performance = (schedule != null) ? schedule.getPerformance() : null;

        LocalDateTime showDateTime = null;
        if (schedule != null && schedule.getShowDate() != null && schedule.getShowTime() != null) {
            showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
        }

        return new MyPageReservationResponseDto(
                reservation.getReservationId(),
                reservation.getReservationNumber(),
                reservation.getCreatedAt(),
                performance != null ? performance.getTitle() : "공연 정보 없음",
                performance != null ? performance.getPosterUrl() : null,
                showDateTime,
                performance != null && performance.getVenue() != null ? performance.getVenue().getName() : "장소 정보 없음",
                reservation.getStatus() != null ? reservation.getStatus().name() : null
        );
    }


    /**
     * 예매대기 상세 정보 DTO 변환
     * @param waiting 데이터베이스에서 꺼내온 사용자의 예매대기 원본 데이터
     * @return 마이페이지용 예매대기 응답 DTO
     */
    private MyPageWaitingResponseDto toMyPageWaitingResponseDto(Waiting waiting) {
        Schedule schedule = waiting.getSchedule();
        Performance performance = (schedule != null) ? schedule.getPerformance() : null;

        LocalDateTime showDateTime = null;
        if (schedule != null && schedule.getShowDate() != null && schedule.getShowTime() != null) {
            showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
        }


         int waitingOrder = 1; // 기본값 1
         if (schedule != null) {
             int aheadCount = waitingRepository.countByScheduleAndCreatedAtBeforeAndStatus(
                     schedule,
                     waiting.getCreatedAt(),
                     WaitingStatus.WAITING
             );
             waitingOrder = aheadCount + 1;
        }

        Seat seat = waiting.getSeat();
        String seatInfo = "좌석 정보 없음";

        if (seat != null) {
            seatInfo = seat.getSeatGrade() + " " +
                    seat.getSectionName() + " " +
                    seat.getRowNum() + "열 " +
                    seat.getSeatNum() + "번";
        }

        return new MyPageWaitingResponseDto(
                waiting.getWaitingId(),
                performance != null ? performance.getTitle() : "공연 정보 없음",
                performance != null ? performance.getPosterUrl() : null,
                showDateTime,
                seatInfo,
                waitingOrder,
                waiting.getStatus() != null ? waiting.getStatus().name() : null
        );
    }


    /**
     * 내 예매대기 목록 조회
     * @param userId 로그인한 사용자의 고유 ID
     * @return 마이페이지용 예매대기 응답 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MyPageWaitingResponseDto> getMyWaitings(Long userId) {
        List<Waiting> waitings = waitingRepository.findByUserId(userId);
        List<MyPageWaitingResponseDto> result = new ArrayList<>();

        for (Waiting waiting : waitings) {
            result.add(toMyPageWaitingResponseDto(waiting));
        }

        return result;
    }


    /**
     * 예매대기 취소 (상태 변경 방식)
     * @param waitingId 취소하고자 하는 예매대기 고유 ID
     */
    @Transactional
    public void cancelWaitingById(Long waitingId) {
        Optional<Waiting> optionalWaiting = waitingRepository.findById(waitingId);

        if (optionalWaiting.isEmpty()) {
        if (optionalWaiting.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 예매대기 내역입니다. ID: " + waitingId);
        }

        Waiting waiting = optionalWaiting.get();
        waiting.changeStatus(WaitingStatus.EXPIRED)
    }



    /**
     * 예매대기 엔티티를 마이페이지 전용 응답 DTO로 변환
     */
    private MyPageWaitingResponseDto toMyPageWaitingResponseDto(Waiting waiting) {
        Schedule schedule = waiting.getSchedule();
        Performance performance = (schedule != null) ? schedule.getPerformance() : null;

        LocalDateTime showDateTime = null;
        if (schedule != null && schedule.getShowDate() != null && schedule.getShowTime() != null) {
            showDateTime = LocalDateTime.of(schedule.getShowDate(), schedule.getShowTime());
        }

        // 대기순번 계산 로직 (임시로 1번 처리, 프로젝트 규칙에 따라 추후 보완 가능)
        int waitingOrder = 1;

        return new MyPageWaitingResponseDto(
                waiting.getWaitingId(),
                performance != null ? performance.getTitle() : "공연 정보 없음",
                performance != null ? performance.getPosterUrl() : null,
                showDateTime,
                "신청 좌석 정보", // 좌석 매핑 구조에 따라 텍스트 결합 필요
                waitingOrder,
                waiting.getStatus() != null ? waiting.getStatus().name() : null
        );
    }




}