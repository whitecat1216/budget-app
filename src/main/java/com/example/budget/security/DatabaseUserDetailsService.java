package com.example.budget.security;

import com.example.budget.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userMapper.findEnabledByEmail(email.trim().toLowerCase());
        if (user == null) throw new UsernameNotFoundException("Invalid email or password");
        user.setScopes(new HashSet<>(userMapper.findScopeIds(user.getId(), user.getTenantId(), "DEPARTMENT")),
                new HashSet<>(userMapper.findScopeIds(user.getId(), user.getTenantId(), "PROJECT")));
        return user;
    }
}
