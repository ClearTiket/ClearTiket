package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.entity.Ranking;
import com.clearticket.clearticket.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {
    final RankingRepository rankingRepository;

    public List<Ranking> getRanking(String period, String genre) {
        if (period == null || period.isEmpty()) period = "daily";
        if (genre == null || genre.isEmpty()) genre = "전체";

        return rankingRepository.findAllByPeriodAndGenreOrderByRanking(period, genre);
    }
}
