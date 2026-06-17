package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/mypage")
public class MypageController {

    private UserSession getLoginUser(HttpSession session) {
        return (UserSession) session.getAttribute("loginUser");
    }

    private boolean checkLogin(HttpSession session, Model model) {
        UserSession user = getLoginUser(session);
        if (user != null) {
            model.addAttribute("loginUser", user);
            return true;
        }
        return false;
    }

    @GetMapping
    public String mypageMain(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/main";
    }

    @GetMapping("/reservations")
    public String reservations(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/reservations";
    }

    @GetMapping("/reservation-detail")
    public String reservationDetail(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/reservation-detail";
    }

    @GetMapping("/cancel-complete")
    public String cancelComplete(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/cancel-complete";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/profile";
    }

    @GetMapping("/address")
    public String address(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        // TODO: 실제 주소 목록 조회 로직으로 교체 필요
        model.addAttribute("addressList", List.of());
        return "mypage/address";
    }

    @GetMapping("/survey")
    public String survey(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        UserSession loginUser = getLoginUser(session);
        model.addAttribute("currentUser", loginUser);
        model.addAttribute("genreList",
                List.of("뮤지컬", "콘서트", "연극", "오페라", "발레", "클래식", "팝페라", "기타"));
        model.addAttribute("moodList",
                List.of("#눈물폭발", "#유머충전", "#감동클라이맥스", "#스트레스해소",
                        "#설렘충전", "#화려한볼거리", "#잔잔한위로", "#생각이많아지는"));
        model.addAttribute("companionList",
                List.of("혼자서", "연인과 함께", "친구와 함께", "가족과 함께", "아이와 함께"));
        return "mypage/survey";
    }

    @GetMapping("/coupons")
    public String coupons(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/coupons";
    }

    @GetMapping("/waitlist")
    public String waitlist(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/waitlist";
    }
}