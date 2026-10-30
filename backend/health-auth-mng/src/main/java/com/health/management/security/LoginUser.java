package com.health.management.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 登录用户认证主体
 */
@Data
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private List<String> roles = new ArrayList<>();
    private List<Long> roleIds = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private Collection<? extends GrantedAuthority> authorities;

    /** 当前登录会话ID（Refresh Token 的 jti） */
    private String sid;

    /** 当前 Access Token 的 jti，用于注销黑名单 */
    private String jti;

    /** 当前 Access Token 过期时间（epoch millis） */
    private Long tokenExpireAt;

    public void buildAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        roles.forEach(r -> list.add(new SimpleGrantedAuthority("ROLE_" + r)));
        permissions.forEach(p -> list.add(new SimpleGrantedAuthority(p)));
        this.authorities = list;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
