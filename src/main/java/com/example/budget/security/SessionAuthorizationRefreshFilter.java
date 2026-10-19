package com.example.budget.security;

import com.example.budget.mapper.UserMapper;
import com.example.budget.model.AppUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SessionAuthorizationRefreshFilter extends OncePerRequestFilter {
    private final UserMapper users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal user
                && !users.securityStateMatches(user.getId(), user.getTenantId(), user.getRole(), user.getAuthVersion())) {
            SecurityContextHolder.clearContext();
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login?sessionExpired=1");
            return;
        }
        chain.doFilter(request, response);
    }
}
