package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.dto.FindEmailRequestDto;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.service.EmailVerificationService;
import com.clearticket.clearticket.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * 이메일 인증 REST 컨트롤러.
 *
 * 회원가입 흐름:
 *  1. POST /api/auth/check-email   → 이메일 중복 확인
 *  2. POST /api/auth/send-code     → 인증번호 발송 (미가입 이메일만)
 *  3. POST /api/auth/verify-code   → 인증번호 검증
 *
 * 이메일 찾기:
 *  POST /api/auth/find-email  → 이름+생년월일 또는 전화번호로 이메일 조회
 *
 * 비밀번호 찾기 흐름:
 *  1. POST /api/auth/check-phone       → 휴대폰 중복 확인 (회원가입 시)
 *  2. POST /api/auth/send-reset-code   → 본인확인 후 인증번호 발송 (가입된 이메일만)
 *  3. POST /api/auth/verify-reset-code → 인증번호 검증
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;

    // 프론트 JS와 동일한 형식 검증 패턴
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String PHONE_REGEX = "^01[0-9]-\\d{3,4}-\\d{4}$";

    /** 세션 키: 회원가입 인증 완료된 이메일 */
    public static final String SESSION_VERIFIED_EMAIL = "verifiedEmail";

    /** 세션 키: 비밀번호 재설정 인증 완료된 이메일 */
    public static final String SESSION_RESET_VERIFIED_EMAIL = "resetVerifiedEmail";

    // ─────────────────────────────────────────────
    // 이메일 중복 확인
    // ─────────────────────────────────────────────

    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("available", false, "message", "이메일을 입력해 주세요."));
        }
        if (!email.matches(EMAIL_REGEX)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("available", false, "message", "올바른 이메일 형식이 아닙니다."));
        }
        if (userService.existsByEmail(email)) {
            return ResponseEntity.ok(Map.of("available", false, "message", "이미 사용 중인 이메일입니다."));
        }
        return ResponseEntity.ok(Map.of("available", true));
    }

    // ─────────────────────────────────────────────
    // 휴대폰 중복 확인
    // ─────────────────────────────────────────────

    @PostMapping("/check-phone")
    public ResponseEntity<Map<String, Object>> checkPhone(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("available", false, "message", "휴대폰 번호를 입력해 주세요."));
        }
        if (!phone.matches(PHONE_REGEX)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("available", false, "message", "올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)"));
        }
        if (userService.existsByPhone(phone)) {
            return ResponseEntity.ok(Map.of("available", false, "message", "이미 사용 중인 휴대폰 번호입니다."));
        }
        return ResponseEntity.ok(Map.of("available", true));
    }

    // ─────────────────────────────────────────────
    // 회원가입 인증번호 발송
    // ─────────────────────────────────────────────

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, Object>> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "이메일을 입력해 주세요."));
        }
        if (!email.matches(EMAIL_REGEX)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "올바른 이메일 형식이 아닙니다."));
        }
        if (userService.existsByEmail(email)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "이미 사용 중인 이메일입니다."));
        }
        try {
            emailVerificationService.sendCode(email);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "이메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }

    // ─────────────────────────────────────────────
    // 회원가입 인증번호 검증
    // ─────────────────────────────────────────────

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Object>> verifyCode(
            @RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String code  = body.get("code");
        if (email == null || code == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("verified", false, "message", "이메일과 인증번호를 입력해 주세요."));
        }
        boolean ok = emailVerificationService.verify(email, code);
        if (ok) {
            session.setAttribute(SESSION_VERIFIED_EMAIL, email);
            return ResponseEntity.ok(Map.of("verified", true));
        }
        return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 일치하지 않거나 만료되었습니다."));
    }

    // ─────────────────────────────────────────────
    // 이메일(아이디) 찾기
    // Body: { "type": "name", "name": "...", "birth": "19990101" }
    //    or { "type": "phone", "phone": "010-1234-5678" }
    // ─────────────────────────────────────────────

    @PostMapping("/find-email")
    public ResponseEntity<Map<String, Object>> findEmail(@RequestBody FindEmailRequestDto dto) {
        Optional<User> userOpt;

        if ("name".equals(dto.getType())) {
            if (dto.getName() == null || dto.getBirth() == null
                    || dto.getName().isBlank() || dto.getBirth().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "이름과 생년월일을 모두 입력해 주세요."));
            }
            userOpt = userService.findByNameAndBirthdate(dto.getName(), dto.getBirth());
        } else if ("phone".equals(dto.getType())) {
            if (dto.getPhone() == null || dto.getPhone().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "전화번호를 입력해 주세요."));
            }
            userOpt = userService.findByPhone(dto.getPhone());
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "올바른 찾기 방식을 선택해 주세요."));
        }

        return userOpt.map(user -> {
            String createdAt = user.getCreatedAt() != null
                    ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                    : null;
            return ResponseEntity.ok(Map.<String, Object>of(
                    "email", maskEmail(user.getEmail()),
                    "createdAt", createdAt != null ? createdAt : ""
            ));
        }).orElseGet(() ->
                ResponseEntity.ok(Map.of("message", "일치하는 회원 정보가 없습니다."))
        );
    }

    // ─────────────────────────────────────────────
    // 비밀번호 찾기: 본인확인 후 인증번호 발송
    // Body: { "email": "...", "verifyType": "name"|"phone", "verifyValue": "..." }
    // ─────────────────────────────────────────────

    @PostMapping("/send-reset-code")
    public ResponseEntity<Map<String, Object>> sendResetCode(@RequestBody Map<String, String> body) {
        String email       = body.get("email");
        String verifyType  = body.get("verifyType");
        String verifyValue = body.get("verifyValue");

        if (email == null || verifyType == null || verifyValue == null
                || email.isBlank() || verifyValue.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "모든 항목을 입력해 주세요."));
        }
        if (!userService.existsByEmail(email)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "가입된 이메일 정보가 없습니다."));
        }
        boolean matched = userService.verifyUserForPasswordReset(email, verifyType, verifyValue);
        if (!matched) {
            return ResponseEntity.ok(Map.of("success", false, "message", "입력하신 정보가 일치하지 않습니다."));
        }
        try {
            emailVerificationService.sendPasswordResetCode(email);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "이메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }

    // ─────────────────────────────────────────────
    // 비밀번호 찾기: 인증번호 검증
    // ─────────────────────────────────────────────

    @PostMapping("/verify-reset-code")
    public ResponseEntity<Map<String, Object>> verifyResetCode(
            @RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String code  = body.get("code");
        if (email == null || code == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("verified", false, "message", "이메일과 인증번호를 입력해 주세요."));
        }
        boolean ok = emailVerificationService.verifyPasswordResetCode(email, code);
        if (ok) {
            session.setAttribute(SESSION_RESET_VERIFIED_EMAIL, email);
            return ResponseEntity.ok(Map.of("verified", true));
        }
        return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 일치하지 않거나 만료되었습니다."));
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) return email;
        String local  = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        return local.substring(0, 2) + "*".repeat(local.length() - 2) + domain;
    }
}