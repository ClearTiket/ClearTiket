package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.CouponResponseDto;
import com.clearticket.clearticket.model.entity.Coupon;
import com.clearticket.clearticket.model.entity.Performance;
import com.clearticket.clearticket.service.CouponService;
import com.clearticket.clearticket.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CouponViewController {

    private final CouponService couponService;
    private final ReservationService reservationService; // 예약/좌석 정보를 전담

    @GetMapping("/coupons")
    public String getCouponView(
            @RequestParam("reservationId") Long reservationId,
            HttpSession session,
            Model model) {

        UserSession loginUser = (UserSession) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";
        Map<String, Object> summary = reservationService.getReservationSummary(reservationId);
        Performance performance = (Performance) summary.get("performance");

        String userId = loginUser.getId();
        List<CouponResponseDto> coupons = couponService.getAvailableCoupons(userId);

        model.addAttribute("reservation", summary.get("reservation"));
        model.addAttribute("performance", performance);
        model.addAttribute("coupons", coupons); // 뷰에서 라디오 버튼으로 사용

        // [★ 수정된 부분] 서비스에서 새로 포장한 등급별 리스트 바구니를 화면(타임리프)으로 던져줍니다!
        model.addAttribute("selectedGrades", summary.get("selectedGrades"));
        model.addAttribute("seatPriceSum", summary.get("seatPriceSum"));

        return "reservation/discount-coupon";
    }

}