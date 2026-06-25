package com.clearticket.clearticket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSession implements Serializable {
    private Long userId;   // ← 추가: DB PK (bookSeat API에 전달할 실제 ID)
    private String id;     // 기존 필드 유지 (email 값이 들어있음)
    private String name;
    private String email;
}