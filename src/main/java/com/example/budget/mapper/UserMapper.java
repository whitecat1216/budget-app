package com.example.budget.mapper;

import com.example.budget.model.AppUserPrincipal;
import com.example.budget.model.TeamUser;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO users (tenant_id, email, display_name, password_hash, role, enabled) VALUES (#{tenantId}, #{email}, #{displayName}, #{passwordHash}, #{role}, FALSE)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertInvite(TeamUser user);

    @Select("SELECT id, email, display_name, role, enabled FROM users WHERE tenant_id = #{tenantId} ORDER BY display_name")
    List<Map<String,Object>> findTeam(@Param("tenantId") long tenantId);

    @Insert("INSERT INTO users (tenant_id, email, display_name, password_hash, role, enabled) VALUES (1, LOWER(#{email}), 'システム管理者', #{passwordHash}, 'TENANT_ADMIN', TRUE) ON CONFLICT (tenant_id, email) DO NOTHING")
    int createBootstrapAdmin(@Param("email") String email, @Param("passwordHash") String passwordHash);

    @Select("SELECT id, tenant_id, email, display_name, password_hash AS password, role FROM users WHERE LOWER(email) = #{email} AND enabled = TRUE")
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "tenant_id", javaType = Long.class),
            @Arg(column = "email", javaType = String.class),
            @Arg(column = "display_name", javaType = String.class),
            @Arg(column = "password", javaType = String.class),
            @Arg(column = "role", javaType = String.class)
    })
    AppUserPrincipal findEnabledByEmail(@Param("email") String email);
}
