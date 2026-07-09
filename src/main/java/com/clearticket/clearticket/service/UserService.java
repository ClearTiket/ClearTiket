package com.clearticket.clearticket.service;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.RegisterRequestDto;
import com.clearticket.clearticket.model.dto.SurveyRequestDto;
import com.clearticket.clearticket.model.entity.Tag;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.model.entity.UserTag;
import com.clearticket.clearticket.repository.TagRepository;
import com.clearticket.clearticket.repository.UserRepository;
import com.clearticket.clearticket.repository.UserTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TagRepository tagRepository;
    private final UserTagRepository userTagRepository;

    private static final String EMAIL_REGEX    = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String PHONE_REGEX     = "^01[0-9]-\\d{3,4}-\\d{4}$";
    private static final String PASSWORD_REGEX  = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,20}$";

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public Optional<UserSession> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> new UserSession(String.valueOf(user.getUserId()), user.getName(), user.getEmail(), user.getPhone()));
    }

    @Transactional
    public User register(RegisterRequestDto dto) {
        if (dto.getEmail() == null || !dto.getEmail().matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
        if (dto.getPhone() == null || !dto.getPhone().matches(PHONE_REGEX)) {
            throw new IllegalArgumentException("올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)");
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

    public Optional<User> findByNameAndBirthdate(String name, String birthdate) {
        return userRepository.findByNameAndBirthdate(name, birthdate);
    }

    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

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

    @Transactional
    public void saveSurvey(String email, SurveyRequestDto dto) {
        userRepository.findByEmail(email).ifPresent(user -> {
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

            // 기존에는 여기서 User의 텍스트 컬럼(preferenceGenre 등)에만 저장하고 끝났는데,
            // 메인페이지 "추천 공연" 기능은 실제로는 user_tags 테이블(UserTag)을 조회합니다.
            // 그런데 user_tags에 값을 넣는 코드가 애플리케이션 어디에도 없어서,
            // 설문을 아무리 성실하게 응답해도 추천 결과는 항상 비어있을 수밖에 없었습니다.
            // → 설문 답변(표시 텍스트)에 해당하는 실제 Tag를 찾아 user_tags를 새로 채워줍니다.
            List<String> selectedDisplayNames = new ArrayList<>();
            if (dto.getGenres() != null) selectedDisplayNames.addAll(dto.getGenres());
            if (dto.getMoods() != null) selectedDisplayNames.addAll(dto.getMoods());
            if (dto.getCompanion() != null && !dto.getCompanion().isBlank()) {
                selectedDisplayNames.add(dto.getCompanion());
            }

            if (!selectedDisplayNames.isEmpty()) {
                List<Tag> matchedTags = tagRepository.findByDisplayNameIn(selectedDisplayNames);

                // 재설문 시 기존 취향 태그를 지우고 새로 반영 (중복 누적 방지)
                userTagRepository.deleteAllByUserUserId(user.getUserId());

                for (Tag tag : matchedTags) {
                    UserTag userTag = UserTag.builder()
                            .user(user)
                            .tag(tag)
                            .aiScore(1.0)
                            .build();
                    userTagRepository.save(userTag);
                }
            }
        });
    }
}