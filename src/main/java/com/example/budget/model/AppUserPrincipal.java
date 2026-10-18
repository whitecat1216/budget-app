package com.example.budget.model;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Getter
public class AppUserPrincipal implements UserDetails {
    private final Long id;
    private final Long tenantId;
    private final String email;
    private final String displayName;
    private final String password;
    private final String role;

    public AppUserPrincipal(Long id, Long tenantId, String email, String displayName, String password, String role) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.displayName = displayName;
        this.password = password;
        this.role = role;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role)); }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
