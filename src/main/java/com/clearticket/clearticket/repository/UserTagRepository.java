package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {
    List<UserTag> findAllByUserUserId(Long userUserId);
}
