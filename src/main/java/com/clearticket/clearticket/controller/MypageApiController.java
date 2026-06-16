package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mypage")
public class MypageApiController {

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
        return "mypage/address";
    }

    @GetMapping("/survey")
    public String survey(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
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
