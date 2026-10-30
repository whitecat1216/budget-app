package com.example.budget.model;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Getter
public class AppUserPrincipal implements UserDetails {
    private final Long id;
    private final Long tenantId;
    private final String email;
    private final String displayName;
    private final String password;
    private final String role;
    private final int authVersion;
    private final boolean mfaEnabled;
    private Set<Long> departmentScopes = Set.of();
    private Set<Long> projectScopes = Set.of();

    public AppUserPrincipal(Long id, Long tenantId, String email, String displayName, String password, String role, Integer authVersion) {
        this(id, tenantId, email, displayName, password, role, authVersion, false);
    }

    public AppUserPrincipal(Long id, Long tenantId, String email, String displayName, String password,
                            String role, Integer authVersion, Boolean mfaEnabled) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.displayName = displayName;
        this.password = password;
        this.role = role;
        this.authVersion = authVersion == null ? 0 : authVersion;
        this.mfaEnabled = Boolean.TRUE.equals(mfaEnabled);
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role)); }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
    public void setScopes(Set<Long> departments, Set<Long> projects) {
        this.departmentScopes = departments == null ? Set.of() : Set.copyOf(departments);
        this.projectScopes = projects == null ? Set.of() : Set.copyOf(projects);
    }
}
