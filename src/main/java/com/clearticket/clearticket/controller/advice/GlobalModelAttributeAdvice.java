package com.clearticket.clearticket.controller.advice;

import com.clearticket.clearticket.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 헤더(fragments/header.html)는 모델에 담긴 "loginUser" 속성으로 로그인 상태를 판단합니다.
 * 문제는 지금까지 각 화면 컨트롤러(Controller)마다 개별적으로
 *   session.getAttribute("loginUser") -> model.addAttribute("loginUser", ...)
 * 코드를 직접 작성해왔다는 점입니다.
 *
 * 그러다 보니 일부 컨트롤러(예: 공연 상세 페이지, 공연장 목록, 랭킹/지역 페이지 등)에서는
 * 이 처리를 깜빡 빠뜨려서, 실제로는 로그인이 되어 있는데도 헤더에는 "로그인/회원가입"이
 * 그대로 노출되는 버그가 발생했습니다. (요청하신 "로그인이 어디는 되고 어디는 안되는" 문제)
 *
 * 이 클래스는 @ControllerAdvice + @ModelAttribute를 이용해서,
 * 모든 화면(@Controller)의 요청 처리 직전에 자동으로 세션의 loginUser를 모델에 넣어줍니다.
 * 즉, 개별 컨트롤러에서 매번 loginUser를 챙기지 않아도 헤더는 항상 동일한 로그인 상태를
 * 보여주게 됩니다. (개별 컨트롤러에 이미 있던 동일한 코드는 그대로 있어도 값이 같으므로 무해합니다.)
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalModelAttributeAdvice {

    @ModelAttribute("loginUser")
    public UserSession loginUser(HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser instanceof UserSession) {
            return (UserSession) loginUser;
        }
        return null;
    }
}
