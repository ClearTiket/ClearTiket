package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {
    List<UserTag> findAllByUserUserId(Long userUserId);

    // 설문을 다시 제출할 때 기존 취향 태그를 정리하고 새로 반영하기 위한 삭제
    void deleteAllByUserUserId(Long userUserId);
}