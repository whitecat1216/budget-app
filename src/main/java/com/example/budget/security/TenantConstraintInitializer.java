package com.example.budget.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Executes the PostgreSQL DO block as one statement; Spring's schema.sql parser splits on semicolons. */
@Component
public class TenantConstraintInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public TenantConstraintInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource resource = new ClassPathResource("db/tenant-constraints.sql");
        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        jdbcTemplate.execute(sql);
    }
}
