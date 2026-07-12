package com.costbuddy.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "true")
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final CurrentUserContext currentUserContext;

    public AuthenticationInterceptor(CurrentUserContext currentUserContext){
        this.currentUserContext = currentUserContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        currentUserContext.require(request);
        return true;
    }
}
