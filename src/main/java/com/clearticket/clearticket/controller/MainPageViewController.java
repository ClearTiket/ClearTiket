package com.clearticket.clearticket.controller;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class MainPageViewController {

    @GetMapping("/")
    public String indexView(HttpSession session, Model model) {
        // 헤더 fragment는 모델의 loginUser 값으로 로그인 상태를 판단합니다.
        // 이전에는 세션 값을 모델에 전달하지 않아 실제로 로그인되어 있어도
        // 헤더에는 항상 "로그인" 링크가 보이고, 그 링크를 누르면 /login 에서
        // 세션이 이미 존재한다고 판단해 /mypage로 리다이렉트되는 불일치가 있었습니다.
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser != null) {
            model.addAttribute("loginUser", loginUser);
        }
        return "main/index";
    }
}