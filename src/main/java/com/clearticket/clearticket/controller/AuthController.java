package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Value("${app.dummy-user.id}")
    private String dummyId;

    @Value("${app.dummy-user.password}")
    private String dummyPassword;

    @Value("${app.dummy-user.name}")
    private String dummyName;

    @Value("${app.dummy-user.email}")
    private String dummyEmail;

    /* ── 인덱스 ─────────────────────────────────────── */
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    /* ── 로그인 ─────────────────────────────────────── */
    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("error", error);
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(
            @RequestParam(required = false) String fillDummy,
            @RequestParam(defaultValue = "") String id,
            @RequestParam(defaultValue = "") String password,
            HttpSession session,
            Model model) {

        if (fillDummy != null) {
            model.addAttribute("prefillId", dummyId);
            model.addAttribute("prefillPw", dummyPassword);
            return "auth/login";
        }

        if (dummyId.equals(id) && dummyPassword.equals(password)) {
            session.setAttribute("loginUser", new UserSession(id, dummyName, dummyEmail));
            return "redirect:/mypage";
        }

        model.addAttribute("error", "아이디 혹은 비밀번호가 없거나 일치하지 않습니다.");
        return "auth/login";
    }

    /* ── 로그아웃 ────────────────────────────────────── */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /* ── 회원가입 ────────────────────────────────────── */
    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit() {
        return "redirect:/survey";
    }

    /* ── 설문 ───────────────────────────────────────── */
    @GetMapping("/survey")
    public String survey() {
        return "auth/survey";
    }

    /* ── 아이디 찾기 ─────────────────────────────────── */
    @GetMapping("/find-id")
    public String findIdForm() {
        return "auth/find-id";
    }

    @GetMapping("/find-id/result")
    public String findIdResult(Model model) {
        model.addAttribute("userId", "hong**3");
        return "auth/find-id-result";
    }

    /* ── 비밀번호 찾기 ───────────────────────────────── */
    @GetMapping("/find-password")
    public String findPasswordForm() {
        return "auth/find-password";
    }

    /* ── 비밀번호 재설정 ─────────────────────────────── */
    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam(defaultValue = "form") String step, Model model) {
        model.addAttribute("step", step);
        return "auth/reset-password";
    }
}