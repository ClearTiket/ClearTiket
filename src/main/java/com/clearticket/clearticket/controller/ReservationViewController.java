package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reservations")
public class ReservationViewController {

     /**
     * 세션에서 로그인한 사용자 정보 조회
     * @param session 현재 요청의 HTTP 세션
     * @return 로그인된 사용자 세션 객체, 미로그인시 null
     */
    private UserSession getLoginUser(HttpSession session) {
        return (UserSession) session.getAttribute("loginUser");
    }

    /**
     * 사용자 로그인 여부 검증 후 로그인 정보 화면 전달
     * @param session 현재 요청의 HTTP 세션
     * @param model 화면에 데이터를 전달하기 위한 Model 객체
     * @return 로그인 성공 시 true, 미로그인시 false
     */
    private boolean checkLogin(HttpSession session, Model model) {
        UserSession user = getLoginUser(session);
        if (user != null) {
            model.addAttribute("loginUser", user);
            return true;
        }
        return false;
    }

    /**
     * 예매 상세 조회
     * @param reservationId 조회할 예매 고유 ID
     * @param session 현재 요청의 HTTP 세션
     * @param model 화면에 예매 ID를 전달하기 위한 Model 객체
     * @return 예매 상세 뷰 경로, 미로그인 시 로그인 페이지 리다이렉트 경로
     */
    @GetMapping("/{reservation_id}")
    public String reservationDetailPage(@PathVariable("reservation_id") Long reservationId, HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";

        // 화면 단에 예매 ID를 넘겨서 자바스크립트가 /api/reservations/{id}를 호출할 수 있게 합니다.
        model.addAttribute("reservationId", reservationId);
        return "mypage/reservation-detail";
    }

    /**
     * 예매 취소 완료
     * @param session 현재 요청의 HTTP 세션
     * @param model 화면에 데이터를 전달하기 위한 Model 객체
     * @return 예매 취소 완료 뷰 경로, 미로그인 시 로그인 페이지 리다이렉트 경로
     */
    @GetMapping("/cancel-complete")
    public String cancelCompletePage(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/cancel-complete";
    }
}