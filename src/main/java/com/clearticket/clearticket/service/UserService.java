package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.RegisterRequestDto;
import com.clearticket.clearticket.model.dto.SurveyRequestDto;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ─────────────────────────────────────────────
    // 이메일 중복 확인
    // ─────────────────────────────────────────────

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ─────────────────────────────────────────────
    // 휴대폰 중복 확인
    // ─────────────────────────────────────────────

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    // ─────────────────────────────────────────────
    // 로그인
    // ─────────────────────────────────────────────

    public Optional<UserSession> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> new UserSession(user.getUserId(), user.getEmail(), user.getName(), user.getEmail()));
    }

    // ─────────────────────────────────────────────
    // 회원가입
    // ─────────────────────────────────────────────

    @Transactional
    public User register(RegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
        }

        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .build();

        return userRepository.save(user);
    }

    // ─────────────────────────────────────────────
    // 이메일로 사용자 조회
    // ─────────────────────────────────────────────

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ─────────────────────────────────────────────
    // 비밀번호 찾기 본인확인
    // ─────────────────────────────────────────────

    public boolean verifyUserForPasswordReset(String email, String verifyType, String verifyValue) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if ("name".equals(verifyType)) {
                        return user.getName().equals(verifyValue);
                    } else if ("phone".equals(verifyType)) {
                        return user.getPhone().equals(verifyValue);
                    }
                    return false;
                })
                .orElse(false);
    }

    // ─────────────────────────────────────────────
    // 비밀번호 재설정
    // ─────────────────────────────────────────────

    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 계정이 없습니다."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─────────────────────────────────────────────
    // 취향 설문 데이터 DB 저장
    // ─────────────────────────────────────────────

    @Transactional
    public void saveSurvey(String email, SurveyRequestDto dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        // 필요 시 설문 저장 로직 추가
    }
}