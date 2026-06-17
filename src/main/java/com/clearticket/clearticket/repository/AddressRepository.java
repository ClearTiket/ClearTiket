package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.Address;
import com.clearticket.clearticket.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    /**
     * 특정 회원이 등록한 모든 배송지 목록 조회
     * @param user 조회할 회원
     * @return 회원의 배송지 리스트
     */
    List<Address> findByUser(User user);

    // 🎯 2. 특정 회원의 '기본 배송지' 딱 하나만 찾아오는 메서드 (예매 시 자동 입력용!)

    /**
     * 기본 배송지로 설정된 주소 조회
     * @param user 조회할 회원
     * @return 기본 배송지 설정 여부가 true인 배송지 엔티티
     */
    Optional<Address> findByUserAndIsDefaultTrue(User user);
}