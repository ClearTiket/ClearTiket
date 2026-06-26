package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.entity.Address;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationViewController {

    private final ReservationService reservationService;

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

    /**
     * 예매 2단계 : 수령/주문자 정보 입력 화면
     * @param reservationId 현재 진행중인 예약 ID
     * @param session 현재 요청 HTTP 세션
     * @param model 화면으로 데이터를 넘겨주기 위한 객체
     * @return
     */
    @GetMapping("/{reservation_id}/buyer-info")
    public String showBuyerInfoPage(
            @PathVariable("reservation_id") Long reservationId,
            HttpSession session,
            Model model) {

        if (!checkLogin(session, model)) return "redirect:/login";

        try {
            java.util.Map<String, Object> summary = reservationService.getReservationSummary(reservationId);
            model.addAllAttributes(summary);

            UserSession loginUser = getLoginUser(session);
            model.addAttribute("user", loginUser);
            Address defaultAddress = reservationService.getDefaultAddressByUserId(Long.valueOf(loginUser.getId()));
            model.addAttribute("address", defaultAddress);

            return "reservation/buyer-info";

        } catch (IllegalArgumentException e) {
            return "redirect:/main";
        }
    }


    /**
     * 예매 3단계 : 결제 방법 최종 선택 화면
     * @param reservationId 현재 진행 중인 예약 고유 ID
     * @param session 현재 요청의 HTTP 세션
     * @param model 화면에 예매 요약 데이터를 전달하기 위한 Model 객체
     * @return 결제 페이지 뷰 경로, 미로그인 시 로그인 페이지 리다이렉트 경로
     */
    @GetMapping("/{reservation_id}/payment")
    public String showPaymentPage(
            @PathVariable("reservation_id") Long reservationId,
            HttpSession session,
            Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";

        try {
            java.util.Map<String, Object> summary = reservationService.getReservationSummary(reservationId);
            model.addAllAttributes(summary);

            model.addAttribute("user", getLoginUser(session));

            return "reservation/payment";

        } catch (IllegalArgumentException e) {
            return "redirect:/main";
        }
    }


    /**
     * 예매 4단계 : 예매 및 결제 최종 완료 화면
     * @param reservationId 예매 완료된 고유 예약 번호
     * @param model 화면에 예매 완료 정보를 전달하기 위한 Model 객체
     * @return 최종 완료 뷰 경로
     */
    @GetMapping("/{reservation_id}/complete")
    public String showCompletePage(
            @PathVariable("reservation_id") Long reservationId,
            Model model) {

        try {
            java.util.Map<String, Object> summary = reservationService.getReservationSummary(reservationId);
            model.addAllAttributes(summary);

            return "reservation/complete";

        } catch (IllegalArgumentException e) {
            return "redirect:/main";
        }
    }

    /**
     * 레몬스퀴지 카드 결제 성공 후 팝업창이 리다이렉트되어 돌아오는 임시페이지
     * @param reservationId
     * @param model
     * @return 임시 페이지
     */
    @GetMapping("/payment/complete/success")
    public String paymentSuccess(
            @RequestParam("reservationId") Long reservationId,
            Model model) {
        model.addAttribute("reservationId", reservationId);
        return "reservation/popup-callback";
    }


}