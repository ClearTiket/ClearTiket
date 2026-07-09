package com.clearticket.clearticket.controller;

import com.clearticket.clearticket.model.UserSession;
import com.clearticket.clearticket.model.dto.MyPageAddressResponseDto;
import com.clearticket.clearticket.model.entity.User;
import com.clearticket.clearticket.repository.UserRepository;
import com.clearticket.clearticket.service.MyPageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MypageViewController {

    private final MyPageService myPageService;
    private final UserRepository userRepository; // 추가: 취향 데이터 조회용

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
    public String reservationDetail(@org.springframework.web.bind.annotation.RequestParam(value = "reservationId", required = false) Long reservationId,
                                     HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        model.addAttribute("reservationId", reservationId);
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

        UserSession loginUser = getLoginUser(session);
        Long userId = Long.parseLong(loginUser.getId());

        List<MyPageAddressResponseDto> addressList = myPageService.getAddressList(userId);
        model.addAttribute("addressList", addressList);

        Map<String, Object> addressListJson = new HashMap<>();
        for (MyPageAddressResponseDto a : addressList) {
            Map<String, Object> item = new HashMap<>();
            item.put("alias", a.getAddressName());
            item.put("name", a.getRecipientName());
            item.put("phone", a.getRecipientPhone());
            item.put("zip", a.getZonecode());
            item.put("addr1", a.getRoadAddress());
            item.put("addr2", a.getDetailAddress());
            item.put("isDefault", a.isDefault());
            addressListJson.put("addr-" + a.getAddressId(), item);
        }
        model.addAttribute("addressListJson", addressListJson);

        return "mypage/address";
    }

    @GetMapping("/survey")
    public String survey(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";

        UserSession loginUser = getLoginUser(session);
        Long userId = Long.parseLong(loginUser.getId());

        // DB에서 저장된 취향값 조회 후 model에 전달
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 장르: "뮤지컬,콘서트" → List<String>
            List<String> savedGenres = (user.getPreferenceGenre() != null && !user.getPreferenceGenre().isBlank())
                    ? Arrays.asList(user.getPreferenceGenre().split(","))
                    : Collections.emptyList();

            // 분위기: "스토리/작품성" → List<String>
            List<String> savedMoods = (user.getPreferenceMood() != null && !user.getPreferenceMood().isBlank())
                    ? Arrays.asList(user.getPreferenceMood().split(","))
                    : Collections.emptyList();

            model.addAttribute("savedGenres", savedGenres);
            model.addAttribute("savedMoods", savedMoods);
            model.addAttribute("savedCompanion", user.getPreferenceCompanion()); // 단일값
        } else {
            model.addAttribute("savedGenres", Collections.emptyList());
            model.addAttribute("savedMoods", Collections.emptyList());
            model.addAttribute("savedCompanion", "");
        }

        return "mypage/survey";
    }


    @GetMapping("/address-popup")
    public String addressPopup(HttpSession session, Model model) {
        // 1. 로그인 체크 (로그인 안 되어 있으면 로그인 페이지로)
        if (!checkLogin(session, model)) return "redirect:/login";

        UserSession loginUser = getLoginUser(session);
        Long userId = Long.parseLong(loginUser.getId());

        // 2. 기존 마이페이지 배송지 조회 로직 그대로 재활용 🎯
        List<MyPageAddressResponseDto> addressList = myPageService.getAddressList(userId);
        model.addAttribute("addressList", addressList);

        // 3. 아까 새로 만든 팝업 전용 HTML 파일 경로 리턴!
        // (만약 templates 폴더 바로 밑에 두셨다면 "address-popup"으로,
        //  reservation 폴더 안에 두셨다면 "reservation/address-popup"으로 경로를 맞춰주세요!)
        return "reservation/address-popup";
    }

    @GetMapping("/coupons")
    public String coupons(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";
        return "mypage/coupons";
    }

    @GetMapping("/waitlist")
    public String waitlist(HttpSession session, Model model) {
        if (!checkLogin(session, model)) return "redirect:/login";

        UserSession loginUser = getLoginUser(session);
        Long userId = Long.parseLong(loginUser.getId());

        List<com.clearticket.clearticket.model.dto.MyPageWaitingResponseDto> waitings = myPageService.getMyWaitings(userId);

        java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd(E) HH:mm", java.util.Locale.KOREAN);
        java.time.format.DateTimeFormatter applyFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd(E) HH:mm", java.util.Locale.KOREAN);

        List<Map<String, Object>> pendingList = new java.util.ArrayList<>();
        List<Map<String, Object>> confirmedList = new java.util.ArrayList<>();
        List<Map<String, Object>> cancelledList = new java.util.ArrayList<>();

        for (com.clearticket.clearticket.model.dto.MyPageWaitingResponseDto w : waitings) {
            Map<String, Object> item = new HashMap<>();
            item.put("waitingId", w.getWaitingId());
            item.put("title", w.getPerformanceTitle());
            item.put("posterUrl", w.getPosterImageUrl());
            item.put("venue", w.getVenueName());
            item.put("performanceDate", w.getShowDateTime() != null ? w.getShowDateTime().format(dateFmt) : "정보 없음");
            item.put("applyDate", w.getAppliedAt() != null ? w.getAppliedAt().format(applyFmt) : "-");
            item.put("seat", w.getSeatInfo());
            item.put("rank", w.getWaitingOrder() + "번째");

            String status = w.getStatus();
            if ("WAITING".equals(status)) {
                pendingList.add(item);
            } else if ("NOTIFIED".equals(status) || "COMPLETED".equals(status)) {
                confirmedList.add(item);
            } else {
                cancelledList.add(item);
            }
        }

        model.addAttribute("pendingList", pendingList);
        model.addAttribute("confirmedList", confirmedList);
        model.addAttribute("cancelledList", cancelledList);

        return "mypage/waitlist";
    }
}