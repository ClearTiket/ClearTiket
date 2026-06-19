package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CouponViewController {

    /**
     * 쿠폰 선택 및 결제 팝업 화면 반환
     * @param session 현재 요청의 HTTP 세션
     * @return 예매/결제 step1 화면인 쿠폰/할인 화면
     */
    @GetMapping("/coupons")
    public String getCouponView(HttpSession session, Model model) {
        UserSession loginUser = (UserSession) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/login";
        }

        // 아직 데이터 없어서 테스트용으로 작성
        // 추후 삭제
        Map<String, Object> fakeReservation = new HashMap<>();
        fakeReservation.put("seatGrade", "선택 좌석");
        fakeReservation.put("price", 154000); // 자바스크립트 에러 방지용 기본 단가

        model.addAttribute("performance", new HashMap<>());
        model.addAttribute("reservation", fakeReservation);

        return "reservation/discount-coupon";
    }
}