package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.entity.Ranking;
import com.clearticket.clearticket.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {
    final RankingRepository rankingRepository;

    public List<Ranking> getRanking(String period, String genre) {
        if (period == null || period.isEmpty()) period = "daily";
        if (genre == null || genre.isEmpty()) genre = "전체";

        List<Ranking> rankings = rankingRepository.findAllByPeriodAndGenreOrderByRanking(period, genre);

        // rankings 테이블은 별도 배치로 갱신되는 스냅샷이라, 이미 공연이 종료된 항목이
        // 그대로 남아있을 수 있다. 화면에 지난 공연이 노출되지 않도록 여기서 한 번 더 걸러준다.
        LocalDate today = LocalDate.now();
        return rankings.stream()
                .filter(r -> r.getPerformance() == null
                        || r.getPerformance().getEndDate() == null
                        || !r.getPerformance().getEndDate().isBefore(today))
                .toList();
    }
}
