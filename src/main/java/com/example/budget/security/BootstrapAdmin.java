package com.example.budget.security;

import com.example.budget.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapAdmin implements ApplicationRunner {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.bootstrap-admin.email:}") private String email;
    @Value("${app.bootstrap-admin.password:}") private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.length() < 12) throw new IllegalStateException("Bootstrap admin requires an email and a password of at least 12 characters.");
        userMapper.createBootstrapAdmin(email.trim(), passwordEncoder.encode(password));
    }
}
