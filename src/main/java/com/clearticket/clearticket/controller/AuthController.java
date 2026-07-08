package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.RegisterRequestDto;
import com.clearticket.clearticket.model.dto.SurveyRequestDto;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    private void addLoginUser(HttpSession session, Model model) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser != null) model.addAttribute("loginUser", loginUser);
    }

    @GetMapping("/mypage/main")
    public String index(HttpSession session) {
        if (session.getAttribute("loginUser") != null) return "redirect:/mypage";
        return "redirect:/login";
    }

    // ─── 로그인 ───────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginForm(HttpSession session, Model model) {
        if (session.getAttribute("loginUser") != null) return "redirect:/";
        addLoginUser(session, model);
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        return userService.login(email, password)
                .map(userSession -> {
                    session.setAttribute("loginUser", userSession);
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "이메일 혹은 비밀번호가 일치하지 않습니다.");
                    return "auth/login";
                });
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ─── 회원가입 ──────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerForm(HttpSession session, Model model) {
        if (session.getAttribute("loginUser") != null) return "redirect:/";
        addLoginUser(session, model);
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @ModelAttribute RegisterRequestDto dto,
            HttpSession session,
            Model model) {

        String verifiedEmail = (String) session.getAttribute(
                EmailVerificationController.SESSION_VERIFIED_EMAIL);

        if (verifiedEmail == null || !verifiedEmail.equals(dto.getEmail())) {
            model.addAttribute("error", "이메일 인증을 완료해 주세요.");
            return "auth/register";
        }

        try {
            User user = userService.register(dto);
            session.removeAttribute(EmailVerificationController.SESSION_VERIFIED_EMAIL);
            session.setAttribute("loginUser",
                    new UserSession(String.valueOf(user.getUserId()), user.getName(), user.getEmail(), user.getPhone()));
            return "redirect:/survey";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    // ─── 취향 설문 (회원가입 직후 / 신규 → 저장값 없음) ──────────────────

    @GetMapping("/survey")
    public String survey(HttpSession session, Model model) {
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);

        model.addAttribute("savedGenres", Collections.emptyList());
        model.addAttribute("savedMoods", Collections.emptyList());
        model.addAttribute("savedCompanion", "");

        return "auth/survey";
    }

    @PostMapping("/survey")
    @ResponseBody
    public String saveSurvey(@RequestBody SurveyRequestDto dto, HttpSession session) {
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) return "FAIL";
        userService.saveSurvey(loginUser.getEmail(), dto);
        return "SUCCESS";
    }

    // ─── 아이디(이메일) 찾기 ───────────────────────────────────────────────

    @GetMapping("/find-id")
    public String findIdForm(HttpSession session, Model model) {
        addLoginUser(session, model);
        return "auth/find-id";
    }

    // ─── 비밀번호 찾기 ─────────────────────────────────────────────────────

    @GetMapping("/find-password")
    public String findPasswordForm(HttpSession session, Model model) {
        addLoginUser(session, model);
        return "auth/find-password";
    }

    // ─── 비밀번호 재설정 ───────────────────────────────────────────────────

    @GetMapping("/reset-password")
    public String resetPasswordForm(HttpSession session, Model model) {
        String resetVerifiedEmail = (String) session.getAttribute(
                EmailVerificationController.SESSION_RESET_VERIFIED_EMAIL);

        if (resetVerifiedEmail == null) {
            return "redirect:/find-password";
        }

        addLoginUser(session, model);
        model.addAttribute("step", "form");
        model.addAttribute("email", resetVerifiedEmail);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session, Model model) {

        String resetVerifiedEmail = (String) session.getAttribute(
                EmailVerificationController.SESSION_RESET_VERIFIED_EMAIL);

        if (resetVerifiedEmail == null || !resetVerifiedEmail.equals(email)) {
            return "redirect:/find-password";
        }

        addLoginUser(session, model);
        try {
            userService.resetPassword(email, password);
            session.removeAttribute(EmailVerificationController.SESSION_RESET_VERIFIED_EMAIL);
            model.addAttribute("step", "complete");
            return "auth/reset-password";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("step", "form");
            model.addAttribute("email", email);
            return "auth/reset-password";
        }
    }
}