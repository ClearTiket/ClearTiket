package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankingRepository extends JpaRepository<Ranking, Integer> {
    List<Ranking> findAllByPeriodAndGenreOrderByRanking(String period, String genre);
}
