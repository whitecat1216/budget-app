package com.example.budget.security;

import com.example.budget.model.AppUserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MfaAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof AppUserPrincipal user && user.isMfaEnabled()) {
            request.getSession(true).setAttribute(MfaGateFilter.MFA_REQUIRED, true);
            response.sendRedirect(request.getContextPath() + "/mfa/challenge");
        } else {
            response.sendRedirect(request.getContextPath() + "/");
        }
    }
}
