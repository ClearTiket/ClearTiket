package com.clearticket.clearticket.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 실제 SMTP 발송을 담당하는 컴포넌트.
 *
 * @Async 는 같은 클래스 내부 호출(self-invocation)에서는 동작하지 않으므로
 * EmailVerificationService 와 별도의 빈으로 분리했다.
 * 이렇게 분리해야 sendCode() / sendPasswordResetCode() 호출이
 * SMTP 전송 완료를 기다리지 않고 즉시 반환된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncMailSender {

    private final JavaMailSender mailSender;

    private static final String FROM_ADDRESS = "tjoeun.jr5@gmail.com";
    private static final String FROM_NAME    = "클리어티켓";

    @Async("mailExecutor")
    public void sendAsync(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_ADDRESS, FROM_NAME);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            // 비동기로 실행되므로 컨트롤러에는 이 실패가 전달되지 않는다.
            // 실패 알림이 필요하면 여기서 재시도 큐 적재 또는 알림 로직을 추가해야 한다.
            log.error("이메일 발송 실패: to={}", toEmail, e);
        }
    }
}