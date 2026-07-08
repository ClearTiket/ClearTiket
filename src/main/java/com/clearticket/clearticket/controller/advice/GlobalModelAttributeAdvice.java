package com.clearticket.clearticket.controller.advice;

import com.clearticket.clearticket.model.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

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