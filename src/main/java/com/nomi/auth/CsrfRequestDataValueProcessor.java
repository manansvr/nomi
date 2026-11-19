package com.nomi.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import java.util.Map;
import java.util.UUID;

@Component
public class CsrfRequestDataValueProcessor implements RequestDataValueProcessor {
    @Override
    public String processAction(HttpServletRequest request, String action, String httpMethod) {
        return action;
    }

    @Override
    public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
        return value;
    }

    @Override
    public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object existingToken = session.getAttribute(CsrfProtectionFilter.SESSION_ATTRIBUTE);
        if (existingToken instanceof String token) return Map.of(CsrfProtectionFilter.REQUEST_PARAMETER, token);
        String token = UUID.randomUUID().toString();
        session.setAttribute(CsrfProtectionFilter.SESSION_ATTRIBUTE, token);
        return Map.of(CsrfProtectionFilter.REQUEST_PARAMETER, token);
    }

    @Override
    public void beforeFormSubmission(HttpServletRequest request) {
    }
}
