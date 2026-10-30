package com.example.budget.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class MfaGateFilter extends OncePerRequestFilter {
    public static final String MFA_REQUIRED = "MFA_REQUIRED";
    private static final Set<String> ALLOWED = Set.of("/mfa/challenge", "/login", "/logout", "/error");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var session = request.getSession(false);
        String path = request.getServletPath();
        if (session != null && Boolean.TRUE.equals(session.getAttribute(MFA_REQUIRED))
                && !ALLOWED.contains(path) && !path.startsWith("/css/") && !path.startsWith("/images/")) {
            response.sendRedirect(request.getContextPath() + "/mfa/challenge");
            return;
        }
        chain.doFilter(request, response);
    }
}
