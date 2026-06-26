package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.service.CaptchaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/captcha")
public class CaptchaApiController {

    private final CaptchaService captchaService;

    public CaptchaApiController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/generate")
    public Map<String, String> generateCaptcha(HttpSession session) {
        String randomCode = captchaService.generateRandomCode(6);
        session.setAttribute("CAPTCHA_CODE", randomCode);
        return Map.of("captchaCode", randomCode);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCaptcha(@RequestBody Map<String, String> request, HttpSession session) {
        String inputCode = request.get("inputCode");
        String sessionCode = (String) session.getAttribute("CAPTCHA_CODE");

        if (sessionCode != null && sessionCode.equals(inputCode)) {
            return ResponseEntity.ok("인증 성공");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 실패");
    }
}
