package com.clearticket.clearticket.config;

import com.clearticket.clearticket.model.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 모든 화면(View) 요청에 대해 세션의 로그인 사용자 정보를
 * 자동으로 Model에 추가해주는 인터셉터.
 *
 * 컨트롤러마다 매번 loginUser를 Model에 직접 넣어주는 코드를
 * 반복하지 않도록 하기 위함. (실수로 누락되면 헤더에서
 * 로그인 상태가 깨져 보이는 문제를 방지)
 *
 * @RestController(JSON API)에는 ModelAndView가 없으므로 자동으로 영향 없음.
 */
public class LoginUserInterceptor implements HandlerInterceptor {

    public static final String SESSION_KEY = "loginUser";

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView == null) {
            return; // @ResponseBody / RestController 등 View가 없는 응답은 스킵
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        Object loginUser = session.getAttribute(SESSION_KEY);
        if (loginUser instanceof UserSession && !modelAndView.getModel().containsKey(SESSION_KEY)) {
            modelAndView.addObject(SESSION_KEY, loginUser);
        }
    }
}