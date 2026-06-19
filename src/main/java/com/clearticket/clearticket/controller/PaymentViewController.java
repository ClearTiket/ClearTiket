package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/payments")
public class PaymentViewController {

    /**
     * 사용자 로그인 여부 검증 후 로그인 정보 화면 전달
     * @param session 현재 요청의 HTTP 세션
     * @param model 화면에 데이터를 전달하기 위한 Model 객체
     * @return 로그인 성공 시 true, 미로그인시 false
     */
    private boolean checkLogin(HttpSession session, Model model) {
        UserSession user = (UserSession) session.getAttribute("loginUser");
        if (user != null) {
            model.addAttribute("loginUser", user);
            return true;
        }
        return false;
    }

    /**
     * 전체 결제 내역 목록 화면 조회(마이페이지 용)
     * @param session 현재 요청의 HTTP 세션
     * @param model   화면에 데이터를 전달하기 위한 Model 객체
     * @return 결제 내역 목록 뷰 경로, 미로그인 시 로그인 페이지 리다이렉트 경로
     */
    @GetMapping
    public String paymentListPage(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/reservations";
    }

    /**
     * 결제 단건 상세 내역 화면 이동
     * @param paymentId 조회할 결제 고유 ID
     * @param session 현재 요청의 HTTP 세션
     * @param model 화면에 결제 ID를 전달하기 위한 Model 객체
     * @return 결제 상세 뷰 경로, 미로그인 시 로그인 페이지 리다이렉트 경로
     */
    @GetMapping("/{payment_id}")
    public String paymentDetailPage(@PathVariable("payment_id") Long paymentId, HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";

        model.addAttribute("paymentId", paymentId);
        return "mypage/reservation-detail";
    }
}