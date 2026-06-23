package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    // 외래키(FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 어떤 회원의 배송지인지 식별 (User 엔티티와 연결)

    // 일반 컬럼들
    @Column(nullable = false, length = 50)
    private String addressName; // 배송지명 (예: 집, 회사)

    @Column(nullable = false, length = 50)
    private String recipientName; // 받는 분 이름

    @Column(nullable = false, length = 13)
    private String recipientPhone; // 연락처 (하이픈 포함)

    @Column(nullable = false, length = 10)
    private String zonecode; // 우편번호

    @Column(nullable = false, length = 255)
    private String roadAddress; // 도로명 주소

    @Column(nullable = false, length = 255)
    private String detailAddress; // 상세 주소

    @Column(nullable = false)
    private boolean isDefault; // 기본 배송지 여부 (true/false)

    // 배송지 정보 수정 기능을 위한 메서드
    public void updateAddress(String addressName, String recipientName, String recipientPhone,
                              String zonecode, String roadAddress, String detailAddress, boolean isDefault) {
        this.addressName = addressName;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zonecode = zonecode;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.isDefault = isDefault;
    }

    // 기본 배송지 설정 변경을 위한 메서드
    public void changeDefaultStatus(boolean isDefault) {
        this.isDefault = isDefault;
    }
}