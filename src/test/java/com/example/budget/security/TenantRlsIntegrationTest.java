package com.example.budget.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class TenantRlsIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static DataSource dataSource;
    static long tenantTwo;
    static long categoryOne;
    static long categoryTwo;

    @BeforeAll
    static void initializeSchemaAndFixtures() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUsername(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        dataSource = ds;
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        String constraints = StreamUtils.copyToString(new ClassPathResource("db/tenant-constraints.sql").getInputStream(), StandardCharsets.UTF_8);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(constraints);
            tenantTwo = scalarLong(statement, "INSERT INTO tenants(name,status) VALUES ('企業B','ACTIVE') RETURNING id");
            categoryOne = scalarLong(statement, "SELECT id FROM categories WHERE tenant_id=1 ORDER BY id LIMIT 1");
            categoryTwo = scalarLong(statement, "INSERT INTO categories(tenant_id,name,type) VALUES (" + tenantTwo + ",'B費用','EXPENSE') RETURNING id");
            statement.execute("INSERT INTO transactions(id,tenant_id,category_id,type,amount) VALUES " +
                    "(10001,1," + categoryOne + ",'EXPENSE',100),(20001," + tenantTwo + "," + categoryTwo + ",'EXPENSE',200)");
            statement.execute("CREATE ROLE budget_rls_test NOLOGIN");
            statement.execute("GRANT SELECT, INSERT ON transactions TO budget_rls_test");
        }
    }

    @Test
    void rlsShowsOnlyTheConfiguredTenantAndRejectsForeignWrites() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET ROLE budget_rls_test");
            statement.execute("SELECT set_config('app.tenant_id','1',false)");
            assertEquals(1, scalarLong(statement, "SELECT count(*) FROM transactions"));
            SQLException failure = assertThrows(SQLException.class, () -> statement.execute(
                    "INSERT INTO transactions(id,tenant_id,category_id,type,amount) VALUES (30001," + tenantTwo + "," + categoryTwo + ",'EXPENSE',300)"));
            assertEquals("42501", failure.getSQLState());
        }
    }

    @Test
    void compositeForeignKeyRejectsCrossTenantCategoryAssociation() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            SQLException failure = assertThrows(SQLException.class, () -> statement.execute(
                    "INSERT INTO transactions(id,tenant_id,category_id,type,amount) VALUES (30002,1," + categoryTwo + ",'EXPENSE',300)"));
            assertEquals("23503", failure.getSQLState());
        }
    }

    private static long scalarLong(Statement statement, String sql) throws SQLException {
        try (var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }
}
