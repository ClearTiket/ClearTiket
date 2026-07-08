package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Integer> {
    // 설문에서 선택한 표시용 텍스트(displayName)들로 실제 Tag 엔티티를 찾기 위한 조회
    List<Tag> findByDisplayNameIn(List<String> displayNames);
}
