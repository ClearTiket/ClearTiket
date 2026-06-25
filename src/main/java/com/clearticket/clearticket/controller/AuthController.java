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

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    private void addLoginUser(HttpSession session, Model model) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser != null) model.addAttribute("loginUser", loginUser);
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("loginUser") != null) return "redirect:/mypage";
        return "redirect:/login";
    }

    // ─── 로그인 ───────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginForm(HttpSession session, Model model) {
        if (session.getAttribute("loginUser") != null) return "redirect:/mypage";
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
                    return "redirect:/mypage";
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
        if (session.getAttribute("loginUser") != null) return "redirect:/mypage";
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
            // userId 포함한 UserSession 생성
            session.setAttribute("loginUser",
                    new UserSession(user.getUserId(), user.getEmail(), user.getName(), user.getEmail()));
            return "redirect:/survey";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    // ─── 취향 설문 ─────────────────────────────────────────────────────────

    @GetMapping("/survey")
    public String survey(HttpSession session, Model model) {
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("currentUser", loginUser);
        model.addAttribute("genreList",
                List.of("뮤지컬", "콘서트", "연극", "오페라", "발레", "클래식", "팝페라", "기타"));
        model.addAttribute("moodList",
                List.of("#눈물폭발", "#유머충전", "#감동클라이맥스", "#스트레스해소",
                        "#설렘충전", "#화려한볼거리", "#잔잔한위로", "#생각이많아지는"));
        model.addAttribute("companionList",
                List.of("혼자서", "연인과 함께", "친구와 함께", "가족과 함께", "아이와 함께"));
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

    @PostMapping("/find-id")
    public String findIdSubmit(@RequestParam String email,
                               HttpSession session, Model model) {
        addLoginUser(session, model);
        return userService.findByEmail(email)
                .map(user -> {
                    model.addAttribute("userId", maskEmail(user.getEmail()));
                    return "auth/find-id-result";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "일치하는 이메일 정보가 없습니다.");
                    return "auth/find-id";
                });
    }

    // ─── 비밀번호 찾기 ─────────────────────────────────────────────────────

    @GetMapping("/find-password")
    public String findPasswordForm(HttpSession session, Model model) {
        addLoginUser(session, model);
        return "auth/find-password";
    }

    // ─── 비밀번호 재설정 ───────────────────────────────────────────────────

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam(required = false) String email,
                                    HttpSession session, Model model) {
        String resetVerifiedEmail = (String) session.getAttribute(
                EmailVerificationController.SESSION_RESET_VERIFIED_EMAIL);

        if (resetVerifiedEmail == null) return "redirect:/find-password";

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

    // ─── 유틸 ──────────────────────────────────────────────────────────────

    private String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) return email;
        String local  = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        return local.substring(0, 2) + "*".repeat(local.length() - 2) + domain;
    }
}