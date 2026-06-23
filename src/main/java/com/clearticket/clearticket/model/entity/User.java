package com.clearticket.clearticket.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    /** 생년월일 8자리 (예: 19990101) */
    @Column(length = 8)
    private String birthdate;

    @Column(nullable = false, unique = true, length = 13)
    private String phone;

    /** 우편번호 */
    @Column(length = 10)
    private String zipcode;

    /** 기본 주소 */
    @Column(length = 255)
    private String address;

    /** 상세 주소 */
    @Column(length = 255)
    private String addressDetail;

    /** 선호 장르 (설문 Q1, 콤마로 구분: 예) 뮤지컬,콘서트 */
    @Column(length = 50)
    private String preferenceGenre;

    /** 선호 분위기 태그 (설문 Q2, 콤마로 구분: 예) #눈물폭발,#웅장한 */
    @Column(length = 100)
    private String preferenceMood;

    /** 주 동반자 (설문 Q3, 단일 선택: 예) 연인과 함께 */
    @Column(length = 20)
    private String preferenceCompanion;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}