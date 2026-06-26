package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // 이메일 찾기: 이름 + 생년월일
    Optional<User> findByNameAndBirthdate(String name, String birthdate);

    // 이메일 찾기: 전화번호
    Optional<User> findByPhone(String phone);
}
