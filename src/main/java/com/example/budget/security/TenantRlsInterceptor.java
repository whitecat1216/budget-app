package com.example.budget.security;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Sets transaction-local tenant context for PostgreSQL RLS on every MyBatis operation. */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class TenantRlsInterceptor implements Interceptor {
    private static final Set<String> GLOBAL_STATEMENTS = Set.of(
            "com.example.budget.mapper.UserMapper.findEnabledByEmail",
            "com.example.budget.mapper.UserMapper.findEnabledByEmailForReset",
            "com.example.budget.mapper.UserMapper.findValidPasswordReset",
            "com.example.budget.mapper.UserMapper.createBootstrapAdmin",
            "com.example.budget.mapper.RegistrationMapper.insertTenantKey",
            "com.example.budget.mapper.RegistrationMapper.findValidVerification",
            "com.example.budget.mapper.RegistrationMapper.enableUser",
            "com.example.budget.mapper.RegistrationMapper.activateTenant",
            "com.example.budget.mapper.AccountingSyncMapper.recoverExpiredRuns",
            "com.example.budget.mapper.AccountingSyncMapper.claimable"
    );

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Executor executor = (Executor) invocation.getTarget();
        Connection connection = executor.getTransaction().getConnection();
        boolean autoCommit = connection.getAutoCommit();
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        Object parameters = invocation.getArgs()[1];
        Long tenantId = tenantId(parameters);
        if (tenantId == null) tenantId = currentTenantOrNull();
        boolean bypass = GLOBAL_STATEMENTS.contains(statement.getId());

        setConfig(connection, "app.tenant_id", tenantId == null ? "" : tenantId.toString(), !autoCommit);
        setConfig(connection, "app.rls_bypass", bypass ? "on" : "off", !autoCommit);
        try {
            return invocation.proceed();
        } finally {
            if (autoCommit) {
                setConfig(connection, "app.rls_bypass", "off", false);
                setConfig(connection, "app.tenant_id", "", false);
            }
        }
    }

    @Override public Object plugin(Object target) { return Plugin.wrap(target, this); }
    @Override public void setProperties(Properties properties) { }

    private Long tenantId(Object parameters) {
        if (parameters instanceof Map<?, ?> map) {
            Object value = map.get("tenantId");
            if (value instanceof Number number) return number.longValue();
        }
        if (parameters != null) {
            try {
                Method getter = parameters.getClass().getMethod("getTenantId");
                Object value = getter.invoke(parameters);
                if (value instanceof Number number) return number.longValue();
            } catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }

    private Long currentTenantOrNull() {
        try { return TenantSession.tenantId(); }
        catch (IllegalStateException e) { return null; }
    }

    private void setConfig(Connection connection, String name, String value, boolean local) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT set_config(?, ?, ?)")) {
            statement.setString(1, name);
            statement.setString(2, value);
            statement.setBoolean(3, local);
            statement.execute();
        }
    }
}
