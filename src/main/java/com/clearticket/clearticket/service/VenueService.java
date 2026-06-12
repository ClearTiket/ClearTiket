package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.dto.performance.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class VenueService {

    // 1. 공연장 기준 관람 가능 날짜 추출 로직 (컨트롤러의 /dates 가 호출함)
    public List<AvailableDateResponse> getDatesByVenueKopisId(String venueKopisId) {
        List<AvailableDateResponse> availableDates = new ArrayList<>();

        // 오늘(2026-06-12, 금요일)부터 딱 일주일(7일간)의 달력 데이터를 생성합니다.
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(7);

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.toString();
            String dayOfWeek = currentDate.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, Locale.KOREAN);

            // 포장 박스(DTO)에 담아서 리스트에 추가!
            availableDates.add(new AvailableDateResponse(dateStr, dayOfWeek, true));
            currentDate = currentDate.plusDays(1);
        }

        return availableDates;
    }

    // 2. 특정 날짜의 회차 목록 추출 로직 (컨트롤러의 /schedules 가 호출함)
    public List<ScheduleResponse> getSchedulesByVenueAndDate(String venueKopisId, LocalDate date) {
        List<ScheduleResponse> schedules = new ArrayList<>();

        //  프론트엔드(Thymeleaf)에서 달력 날짜를 클릭했을 때 1회차, 2회차 버튼이 화면에 그려질 수 있도록
        // ERD 스케줄 테이블 컬럼 규격에 맞춘 가짜 데이터를 든든하게 장전해 둡니다.
        schedules.add(new ScheduleResponse(501L, 1, "14:00")); // schedule_id: 501, round_number: 1, show_time: 14:00
        schedules.add(new ScheduleResponse(502L, 2, "19:30")); // schedule_id: 502, round_number: 2, show_time: 19:30

        return schedules;
    }

    public VenueInfoResponse getVenueInfoByKopisId(String venueKopisId) {
        //  지금은 샘플 데이터 장전! 나중에 팀원분 데이터가 DB에 들어오면 Repository 조회로 교체할 곳!
        if ("FC001234".equals(venueKopisId) || venueKopisId == null) {
            return new VenueInfoResponse(
                    "샤롯데씨어터",
                    1241,
                    "서울특별시 송파구 올림픽로 240 (잠실동)",
                    "1644-0077",
                    "http://www.charlottetheatre.co.kr/"
            );
        }

        // 다른 ID가 들어왔을 때의 기본 샘플 데이터
        return new VenueInfoResponse(
                "예술의전당 오페라극장",
                2340,
                "서울특별시 서초구 서초동 700",
                "02-580-1300",
                "https://www.sac.or.kr/"
        );

    }

    public List<CastingResponse> getCastingInfoByKopisId(String venueKopisId) {
        List<CastingResponse> castingList = new ArrayList<>();

        castingList.add(new CastingResponse("조승우"));
        castingList.add(new CastingResponse("홍광호"));
        castingList.add(new CastingResponse("박은태"));

        return castingList;
    }

    public ReviewListResponse getReviewsByVenueKopisId(String venueKopisId, int page) {
        List<ReviewItem> allReviews = new ArrayList<>();

        allReviews.add(new ReviewItem(1L, "티켓마스터","사운드가 빵빵해서 너무 좋았어요!", 5, "2026-06-10"));
        allReviews.add(new ReviewItem(2L, "코딩천재","좌석 간격이 조금 좁아서 아쉽습니다.", 3, "2026-06-11"));
        allReviews.add(new ReviewItem(3L, "뮤지컬덕후","시야 방해 없고 무대가 한눈에 보여요!", 4, "2026-06-13"));

        // 샘플 데이터라 수동으로 최신글순 정렬
        List<ReviewItem> sortedReview = new ArrayList<>();
        sortedReview.add(allReviews.get(2));
        sortedReview.add(allReviews.get(1));
        sortedReview.add(allReviews.get(0));

        int totalSum = 0;
        for (ReviewItem review :  sortedReview) {
            totalSum += review.getRating();
        }

        double avgRating = 
    }
}