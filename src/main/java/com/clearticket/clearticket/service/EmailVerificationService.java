package com.clearticket.clearticket.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이메일 인증번호 발송 및 검증 서비스.
 * 인증코드는 인메모리(ConcurrentHashMap)에 저장하며 5분간 유효합니다.
 * 운영 환경에서는 Redis로 교체를 권장합니다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender mailSender;

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    private static final int EXPIRE_MINUTES = 5;
    private static final String FROM_ADDRESS = "tjoeun.jr5@gmail.com";
    private static final String FROM_NAME    = "클리어티켓";

    // ─────────────────────────────────────────────
    // 인증번호 발송 (회원가입용 - 미가입 이메일만)
    // ─────────────────────────────────────────────

    public void sendCode(String toEmail) {
        String code = generateCode();
        codeStore.put(toEmail, new CodeEntry(code, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES)));
        sendMail(toEmail, code);
    }

    // ─────────────────────────────────────────────
    // 인증번호 발송 (비밀번호 찾기용 - 가입된 이메일만)
    // ─────────────────────────────────────────────

    public void sendPasswordResetCode(String toEmail) {
        String code = generateCode();
        codeStore.put("reset:" + toEmail, new CodeEntry(code, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES)));
        sendMail(toEmail, code);
    }

    // ─────────────────────────────────────────────
    // 인증번호 검증 (회원가입용)
    // ─────────────────────────────────────────────

    public boolean verify(String email, String code) {
        return verifyByKey(email, code);
    }

    // ─────────────────────────────────────────────
    // 인증번호 검증 (비밀번호 찾기용)
    // ─────────────────────────────────────────────

    public boolean verifyPasswordResetCode(String email, String code) {
        return verifyByKey("reset:" + email, code);
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    private boolean verifyByKey(String key, String code) {
        CodeEntry entry = codeStore.get(key);
        if (entry == null) return false;
        if (entry.expireAt().isBefore(LocalDateTime.now())) {
            codeStore.remove(key);
            return false;
        }
        boolean ok = entry.code().equals(code);
        if (ok) codeStore.remove(key);
        return ok;
    }

    private String generateCode() {
        SecureRandom rnd = new SecureRandom();
        int num = rnd.nextInt(900_000) + 100_000;
        return String.valueOf(num);
    }

    private void sendMail(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_ADDRESS, FROM_NAME);
            helper.setTo(toEmail);
            helper.setSubject("[클리어티켓] 이메일 인증번호 안내");
            helper.setText(buildHtmlBody(code), true);

            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    private String buildHtmlBody(String code) {
        return """
                <div style="font-family:'Noto Sans KR',sans-serif;max-width:480px;margin:0 auto;
                            padding:40px 32px;border:1.5px solid #E0E4F5;border-radius:16px;">
                  <h2 style="color:#4B6EF5;margin-bottom:8px;">클리어티켓 이메일 인증</h2>
                  <p style="color:#555;font-size:14px;line-height:1.7;">
                    아래 인증번호를 입력창에 입력해 주세요.<br>
                    인증번호는 발송 후 <strong>5분간</strong> 유효합니다.
                  </p>
                  <div style="margin:28px 0;padding:20px;background:#F3F5FF;border-radius:12px;
                              text-align:center;letter-spacing:6px;font-size:32px;font-weight:700;
                              color:#4B6EF5;">
                    %s
                  </div>
                  <p style="color:#999;font-size:12px;">
                    본인이 요청하지 않은 경우 이 메일을 무시하세요.
                  </p>
                </div>
                """.formatted(code);
    }

    private record CodeEntry(String code, LocalDateTime expireAt) {}
}