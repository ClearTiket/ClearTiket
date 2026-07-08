package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Integer> {
    List<Tag> findByDisplayNameIn(List<String> displayNames);
}