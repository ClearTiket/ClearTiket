package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
