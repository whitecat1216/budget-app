package com.example.budget.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final AuthenticationRateLimitFilter rateLimitFilter;
    private final SessionAuthorizationRefreshFilter sessionAuthorizationRefreshFilter;

    public SecurityConfig(AuthenticationRateLimitFilter rateLimitFilter, SessionAuthorizationRefreshFilter sessionAuthorizationRefreshFilter) {
        this.rateLimitFilter = rateLimitFilter;
        this.sessionAuthorizationRefreshFilter = sessionAuthorizationRefreshFilter;
    }

    @Bean
    FilterRegistrationBean<AuthenticationRateLimitFilter> disableContainerRegistration(AuthenticationRateLimitFilter filter) {
        FilterRegistrationBean<AuthenticationRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<SessionAuthorizationRefreshFilter> disableSessionFilterContainerRegistration(SessionAuthorizationRefreshFilter filter) {
        FilterRegistrationBean<SessionAuthorizationRefreshFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/verify-email", "/accept-invitation", "/forgot-password", "/reset-password", "/error", "/css/**", "/images/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(sessionAuthorizationRefreshFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
}
