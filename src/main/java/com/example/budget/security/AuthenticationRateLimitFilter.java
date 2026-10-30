package com.example.budget.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** A small per-process guard for public authentication endpoints. Use a shared limiter in multi-instance production. */
@Component
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private record Bucket(long startedAt, AtomicInteger count) { }
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) { chain.doFilter(request, response); return; }
        String path = request.getServletPath();
        int max; long window;
        switch (path) {
            case "/login" -> { max = 10; window = Duration.ofMinutes(10).toMillis(); }
            case "/register" -> { max = 5; window = Duration.ofHours(1).toMillis(); }
            case "/forgot-password" -> { max = 5; window = Duration.ofHours(1).toMillis(); }
            case "/reset-password" -> { max = 10; window = Duration.ofHours(1).toMillis(); }
            case "/mfa/challenge", "/mfa/confirm", "/mfa/disable" -> { max = 8; window = Duration.ofMinutes(10).toMillis(); }
            case "/team/invite" -> { max = 10; window = Duration.ofHours(1).toMillis(); }
            default -> { chain.doFilter(request, response); return; }
        }
        String key = path + ":" + request.getRemoteAddr();
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.compute(key, (ignored, current) -> current == null || now - current.startedAt() >= window
                ? new Bucket(now, new AtomicInteger()) : current);
        if (bucket.count().incrementAndGet() > max) {
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("しばらく待ってから、もう一度お試しください。");
            return;
        }
        if (buckets.size() > 10000) buckets.entrySet().removeIf(entry -> now - entry.getValue().startedAt() > Duration.ofHours(2).toMillis());
        chain.doFilter(request, response);
    }
}
