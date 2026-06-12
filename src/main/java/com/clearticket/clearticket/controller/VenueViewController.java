package com.clearticket.clearticket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller // 💡 데이터가 아니라 'HTML 화면'을 보여주는 전용 스티커!
public class VenueViewController {

    @GetMapping("/hall/{mt10id}/detail") // ➔ 브라우저 주소창에 칠 주소!
    public String showVenueDetail(@PathVariable("mt10id") String mt10id, Model model) {

        // 타임리프 HTML에 KOPIS ID 배달 가방 쏙 넣어주기
        model.addAttribute("venueKopisId", mt10id);

        // templates/performances/performance-detail.html 화면을 열어라!
        return "performances/performance-detail";
    }
}
