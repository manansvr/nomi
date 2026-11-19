package com.nomi.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Order(2)
public class CsrfProtectionFilter extends OncePerRequestFilter {
    static final String SESSION_ATTRIBUTE = CsrfProtectionFilter.class.getName() + ".token";
    static final String REQUEST_PARAMETER = "_csrf";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && !isValidToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isValidToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        Object savedToken = session.getAttribute(SESSION_ATTRIBUTE);
        if (!(savedToken instanceof String token)) return false;
        String submittedToken = request.getParameter(REQUEST_PARAMETER);
        if (submittedToken == null) submittedToken = request.getHeader("X-CSRF-TOKEN");
        return submittedToken != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), submittedToken.getBytes(StandardCharsets.UTF_8));
    }
}
