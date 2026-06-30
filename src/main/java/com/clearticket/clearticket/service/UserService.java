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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ─────────────────────────────────────────────
    // 검증 정규식 (프론트 JS와 동일한 패턴)
    // ─────────────────────────────────────────────

    private static final String EMAIL_REGEX     = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String PHONE_REGEX      = "^01[0-9]-\\d{3,4}-\\d{4}$";
    private static final String PASSWORD_REGEX   = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,20}$";
    // YYYYMMDD, 연도 1900~2099, 월 01~12, 일 01~31 형식까지만 1차로 거름 (실존 날짜 여부는 isRealDate에서 추가 확인)
    private static final String BIRTHDATE_REGEX  = "^(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])$";

    // ─────────────────────────────────────────────
    // 중복 확인
    // ─────────────────────────────────────────────

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    // ─────────────────────────────────────────────
    // 로그인
    // ─────────────────────────────────────────────

    public Optional<UserSession> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> new UserSession(String.valueOf(user.getUserId()), user.getName(), user.getEmail(), user.getPhone()));
    }

    // ─────────────────────────────────────────────
    // 회원가입
    // ─────────────────────────────────────────────

    @Transactional
    public User register(RegisterRequestDto dto) {
        if (dto.getEmail() == null || !dto.getEmail().matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
        if (dto.getPhone() == null || !dto.getPhone().matches(PHONE_REGEX)) {
            throw new IllegalArgumentException("올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)");
        }
        if (dto.getBirthdate() == null || !dto.getBirthdate().matches(BIRTHDATE_REGEX)
                || !isRealDate(dto.getBirthdate())) {
            throw new IllegalArgumentException("올바른 생년월일이 아닙니다. (예: 20010101)");
        }
        if (existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (existsByPhone(dto.getPhone())) {
            throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
        }
        if (dto.getPassword() == null || !dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        if (!dto.getPassword().matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException("비밀번호는 8~20자의 영문 대/소문자, 숫자, 특수문자 조합이어야 합니다.");
        }

        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .birthdate(dto.getBirthdate())
                .phone(dto.getPhone())
                .zipcode(dto.getZipcode())
                .address(dto.getAddress())
                .addressDetail(dto.getAddressDetail())
                .build();

        return userRepository.save(user);
    }

    /**
     * 정규식으로 거른 YYYYMMDD 문자열이 실제로 존재하는 날짜인지(2/30, 4/31 같은 값 차단)
     * + 미래 날짜가 아닌지 확인
     */
    private boolean isRealDate(String birthdate) {
        try {
            LocalDate date = LocalDate.parse(birthdate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return !date.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false; // 2/30, 4/31 등 정규식은 통과해도 실존하지 않는 날짜
        }
    }

    // ─────────────────────────────────────────────
    // 이메일(아이디) 찾기
    // ─────────────────────────────────────────────

    /** 이름 + 생년월일로 유저 조회 */
    public Optional<User> findByNameAndBirthdate(String name, String birthdate) {
        return userRepository.findByNameAndBirthdate(name, birthdate);
    }

    /** 전화번호로 유저 조회 */
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    /** 이메일로 유저 조회 (기존) */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ─────────────────────────────────────────────
    // 비밀번호 찾기: 본인 확인
    // verifyType: "name" → 이름 일치 확인
    //             "phone" → 전화번호 일치 확인
    // ─────────────────────────────────────────────

    public boolean verifyUserForPasswordReset(String email, String verifyType, String verifyValue) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();

        return switch (verifyType) {
            case "name"  -> user.getName() != null && user.getName().equals(verifyValue);
            case "phone" -> user.getPhone() != null && user.getPhone().equals(verifyValue);
            default      -> false;
        };
    }

    // ─────────────────────────────────────────────
    // 비밀번호 재설정
    // ─────────────────────────────────────────────

    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입된 이메일 정보가 없습니다."));

        if (!newPassword.matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException("비밀번호는 8~20자의 영문 대/소문자, 숫자, 특수문자 조합이어야 합니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─────────────────────────────────────────────
    // 설문 저장
    // ─────────────────────────────────────────────

    @Transactional
    public void saveSurvey(Long userId, SurveyRequestDto dto) {
        userRepository.findById(userId).ifPresent(user -> {
            if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
                user.setPreferenceGenre(String.join(",", dto.getGenres()));
            }
            if (dto.getMoods() != null && !dto.getMoods().isEmpty()) {
                user.setPreferenceMood(String.join(",", dto.getMoods()));
            }
            if (dto.getCompanion() != null && !dto.getCompanion().isBlank()) {
                user.setPreferenceCompanion(dto.getCompanion());
            }
            userRepository.save(user);
        });
    }
}