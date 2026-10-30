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

    @Select("SELECT u.id, u.email, u.display_name, u.role, u.enabled, " +
            "COALESCE((SELECT '|' || string_agg(s.scope_id::text, '|') || '|' FROM user_scopes s WHERE s.tenant_id=u.tenant_id AND s.user_id=u.id AND s.scope_type='DEPARTMENT'), '|') AS department_scope_ids, " +
            "COALESCE((SELECT '|' || string_agg(s.scope_id::text, '|') || '|' FROM user_scopes s WHERE s.tenant_id=u.tenant_id AND s.user_id=u.id AND s.scope_type='PROJECT'), '|') AS project_scope_ids " +
            "FROM users u WHERE u.tenant_id = #{tenantId} ORDER BY u.display_name")
    List<Map<String,Object>> findTeam(@Param("tenantId") long tenantId);

    @Update("UPDATE users SET role = #{role}, auth_version=auth_version+1 WHERE id = #{id} AND tenant_id = #{tenantId} AND role <> 'TENANT_ADMIN'")
    int updateRole(@Param("id") long id, @Param("tenantId") long tenantId, @Param("role") String role);

    @Update("UPDATE users SET enabled = #{enabled}, auth_version=auth_version+1 WHERE id = #{id} AND tenant_id = #{tenantId} AND role <> 'TENANT_ADMIN'")
    int setEnabled(@Param("id") long id, @Param("tenantId") long tenantId, @Param("enabled") boolean enabled);

    @Select("SELECT COUNT(*) FROM users WHERE tenant_id = #{tenantId} AND role = 'TENANT_ADMIN' AND enabled = TRUE")
    int countEnabledAdmins(@Param("tenantId") long tenantId);

    @Select("SELECT COUNT(*) FROM users WHERE id = #{id} AND tenant_id = #{tenantId}")
    int countTenantUser(@Param("id") long id, @Param("tenantId") long tenantId);

    @Select("SELECT COUNT(*) > 0 FROM email_verifications WHERE user_id=#{userId} AND purpose='INVITE' AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP")
    boolean isInvitePending(@Param("userId") long userId);

    @Update("UPDATE users SET auth_version=auth_version+1 WHERE id=#{userId} AND tenant_id=#{tenantId}")
    int bumpAuthVersion(@Param("tenantId") long tenantId, @Param("userId") long userId);

    @Delete("DELETE FROM user_scopes WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    int clearScopes(@Param("tenantId") long tenantId, @Param("userId") long userId);

    @Insert("<script>INSERT INTO user_scopes (tenant_id, user_id, scope_type, scope_id) SELECT #{tenantId}, #{userId}, 'DEPARTMENT', d.id FROM departments d WHERE d.tenant_id = #{tenantId} AND d.id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> ON CONFLICT DO NOTHING</script>")
    int assignDepartmentScopes(@Param("tenantId") long tenantId, @Param("userId") long userId, @Param("ids") List<Long> ids);

    @Insert("<script>INSERT INTO user_scopes (tenant_id, user_id, scope_type, scope_id) SELECT #{tenantId}, #{userId}, 'PROJECT', p.id FROM projects p WHERE p.tenant_id = #{tenantId} AND p.id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> ON CONFLICT DO NOTHING</script>")
    int assignProjectScopes(@Param("tenantId") long tenantId, @Param("userId") long userId, @Param("ids") List<Long> ids);

    @Select("SELECT scope_id FROM user_scopes WHERE user_id = #{userId} AND tenant_id = #{tenantId} AND scope_type = #{type}")
    List<Long> findScopeIds(@Param("userId") long userId, @Param("tenantId") long tenantId, @Param("type") String type);

    @Select("SELECT id, tenant_id FROM users WHERE LOWER(email) = #{email} AND enabled = TRUE")
    @Results({@Result(property="id", column="id"), @Result(property="tenantId", column="tenant_id")})
    PasswordResetTarget findEnabledByEmailForReset(@Param("email") String email);

    @Insert("INSERT INTO email_verifications (user_id, token_hash, purpose, expires_at) VALUES (#{userId}, #{tokenHash}, 'PASSWORD_RESET', #{expiresAt})")
    int insertPasswordReset(@Param("userId") long userId, @Param("tokenHash") String tokenHash, @Param("expiresAt") java.time.LocalDateTime expiresAt);

    @Select("SELECT u.id AS user_id, u.tenant_id, v.id AS verification_id FROM email_verifications v JOIN users u ON u.id = v.user_id WHERE v.token_hash = #{tokenHash} AND v.purpose = 'PASSWORD_RESET' AND v.used_at IS NULL AND v.expires_at > CURRENT_TIMESTAMP AND u.enabled = TRUE")
    @Results({@Result(property="userId", column="user_id"), @Result(property="tenantId", column="tenant_id"), @Result(property="verificationId", column="verification_id")})
    com.example.budget.mapper.RegistrationMapper.VerificationTarget findValidPasswordReset(@Param("tokenHash") String tokenHash);

    @Update("UPDATE users SET password_hash = #{passwordHash}, auth_version=auth_version+1 WHERE id = #{userId} AND tenant_id = #{tenantId} AND enabled = TRUE")
    int updatePassword(@Param("userId") long userId, @Param("tenantId") long tenantId, @Param("passwordHash") String passwordHash);

    @Insert("INSERT INTO users (tenant_id, email, display_name, password_hash, role, enabled) VALUES (1, LOWER(#{email}), 'システム管理者', #{passwordHash}, 'TENANT_ADMIN', TRUE) ON CONFLICT (tenant_id, email) DO NOTHING")
    int createBootstrapAdmin(@Param("email") String email, @Param("passwordHash") String passwordHash);

    @Select("SELECT id, tenant_id, email, display_name, password_hash AS password, role, auth_version, mfa_enabled FROM users WHERE LOWER(email) = #{email} AND enabled = TRUE")
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "tenant_id", javaType = Long.class),
            @Arg(column = "email", javaType = String.class),
            @Arg(column = "display_name", javaType = String.class),
            @Arg(column = "password", javaType = String.class),
            @Arg(column = "role", javaType = String.class),
            @Arg(column = "auth_version", javaType = Integer.class),
            @Arg(column = "mfa_enabled", javaType = Boolean.class)
    })
    AppUserPrincipal findEnabledByEmail(@Param("email") String email);

    @Select("SELECT COUNT(*)=1 FROM users WHERE id=#{id} AND tenant_id=#{tenantId} AND role=#{role} AND auth_version=#{authVersion} AND enabled=TRUE")
    boolean securityStateMatches(@Param("id") long id, @Param("tenantId") long tenantId, @Param("role") String role, @Param("authVersion") int authVersion);

    @Select("SELECT mfa_enabled FROM users WHERE id=#{userId} AND tenant_id=#{tenantId}")
    boolean mfaEnabled(@Param("tenantId") long tenantId, @Param("userId") long userId);

    @Select("SELECT mfa_secret_ciphertext FROM users WHERE id=#{userId} AND tenant_id=#{tenantId} AND mfa_enabled=TRUE")
    String mfaSecret(@Param("tenantId") long tenantId, @Param("userId") long userId);

    @Update("UPDATE users SET mfa_enabled=TRUE, mfa_secret_ciphertext=#{ciphertext} WHERE id=#{userId} AND tenant_id=#{tenantId} AND enabled=TRUE")
    int enableMfa(@Param("tenantId") long tenantId, @Param("userId") long userId, @Param("ciphertext") String ciphertext);

    @Update("UPDATE users SET mfa_enabled=FALSE, mfa_secret_ciphertext=NULL WHERE id=#{userId} AND tenant_id=#{tenantId}")
    int disableMfa(@Param("tenantId") long tenantId, @Param("userId") long userId);

    class PasswordResetTarget {
        private Long id; private Long tenantId;
        public Long getId() { return id; } public void setId(Long id) { this.id = id; }
        public Long getTenantId() { return tenantId; } public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    }
}
