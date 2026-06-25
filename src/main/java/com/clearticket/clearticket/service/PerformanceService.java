package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    final PerformanceRepository performanceRepository;

    int defaultPageSize = 20;

    Pageable validPageable(Integer page, Integer pageSize) {
        if (page == null || page <= 0) page = 1;
        page--;
        Pageable pageable = PageRequest.of(page, pageSize);
        return pageable;
    }
    Pageable validPageable(Integer page) {
        return validPageable(page, defaultPageSize);
    }

    public Page<Performance> findAll(Integer page, Integer pageSize) {
        return performanceRepository.findAll(validPageable(page, pageSize));
    }
    public Page<Performance> findAll(int page) {
        return findAll(page, defaultPageSize);
    }


    public Page<Performance> findAllByRegion(String region, int page) {

        List<String> regions = new ArrayList<>();
        switch (region) {
            // TODO: 지역 텍스트 형식이 Venue 테이블과 Performances가 서로 다름 -> 이거 해결해야됨
            //case "충청" -> regions.addAll(Arrays.asList("충북", "충남", "대전", "세종"));
            //case "전라" -> regions.addAll(Arrays.asList("전북", "전남", "광주"));
            //case "경상" -> regions.addAll(Arrays.asList("경북", "경남", "부산", "대구", "울산"));
            case "서울" -> regions.add("서울특별시");
            case "충청" -> regions.addAll(Arrays.asList("충청북도", "충청남도", "대전광역시", "세종특별자치시"));
            case "전라" -> regions.addAll(Arrays.asList("전북특별자치도", "전라남도", "광주광역시"));
            case "경상" -> regions.addAll(Arrays.asList("경상북도", "경상남도", "부산광역시", "대구광역시", "울산광역시"));
            default -> regions.add(region);
        }

        return performanceRepository.findAllByRegionIn(regions, validPageable(page));
    }

    public Page<Performance> findRankingAllByGenre(String genre, Integer page) {
        // TODO: 현재는 단순 장르 필터링만 -> 예매율 계산 + 정렬 로직 작성 필요
        return performanceRepository.findAllByGenre(genre, validPageable(page, 10));
    }
}
