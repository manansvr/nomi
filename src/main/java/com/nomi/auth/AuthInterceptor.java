package com.nomi.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") instanceof AuthUser) return true;

        String path = request.getRequestURI();
        if (path.startsWith("/auth") || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.startsWith("/webjars/")
                || path.equals("/favicon.ico") || path.equals("/error")) return true;

        String next = path;
        if (request.getQueryString() != null) next += "?" + request.getQueryString();
        response.sendRedirect("/auth/sign-in?next="
                + java.net.URLEncoder.encode(next, java.nio.charset.StandardCharsets.UTF_8));
        return false;
    }
}
