package com.example.budget.mapper;

import org.apache.ibatis.annotations.*;
import com.example.budget.service.RegistrationService;
import java.time.LocalDateTime;

@Mapper
public interface RegistrationMapper {
    @Insert("INSERT INTO tenants (name, status, setup_completed) VALUES (#{companyName}, 'PENDING_VERIFICATION', FALSE)")
    @Options(useGeneratedKeys = true, keyProperty = "tenantId")
    int insertTenantKey(RegistrationService.TenantKey key);

    @Insert("INSERT INTO users (tenant_id, email, display_name, password_hash, role, enabled) VALUES (#{tenantId}, #{email}, #{displayName}, #{passwordHash}, 'TENANT_ADMIN', FALSE)")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int insertUserKey(RegistrationService.UserKey key);

    @Insert("INSERT INTO email_verifications (user_id, token_hash, purpose, expires_at) VALUES (#{userId}, #{tokenHash}, #{purpose}, #{expiresAt})")
    int insertVerification(@Param("userId") Long userId, @Param("tokenHash") String tokenHash,
                           @Param("expiresAt") LocalDateTime expiresAt, @Param("purpose") String purpose);

    @Select("SELECT u.id AS user_id, u.tenant_id, v.id AS verification_id FROM email_verifications v JOIN users u ON u.id = v.user_id WHERE v.token_hash = #{tokenHash} AND v.purpose = #{purpose} AND v.used_at IS NULL AND v.expires_at > CURRENT_TIMESTAMP AND u.enabled = FALSE")
    @Results({@Result(property = "userId", column = "user_id"), @Result(property = "tenantId", column = "tenant_id"), @Result(property = "verificationId", column = "verification_id")})
    VerificationTarget findValidVerification(@Param("tokenHash") String tokenHash, @Param("purpose") String purpose);

    @Update("UPDATE email_verifications SET used_at = CURRENT_TIMESTAMP WHERE id = #{id} AND purpose = #{purpose} AND used_at IS NULL")
    int consumeVerification(@Param("id") long id, @Param("purpose") String purpose);

    @Update("UPDATE users SET enabled = TRUE WHERE id = #{id} AND enabled = FALSE")
    int enableUser(long id);

    @Update("UPDATE tenants SET status = 'ACTIVE' WHERE id = #{id} AND status = 'PENDING_VERIFICATION'")
    int activateTenant(long id);

    @Update("UPDATE users SET password_hash = #{passwordHash}, enabled = TRUE WHERE id = #{userId} AND tenant_id = #{tenantId} AND enabled = FALSE")
    int activateInvitation(@Param("userId") long userId, @Param("tenantId") long tenantId, @Param("passwordHash") String passwordHash);

    class VerificationTarget {
        private Long userId;
        private Long tenantId;
        private Long verificationId;
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        public Long getVerificationId() { return verificationId; }
        public void setVerificationId(Long verificationId) { this.verificationId = verificationId; }
    }
}
