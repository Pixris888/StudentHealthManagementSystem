package com.health.management.security;

import com.health.management.common.exception.ServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 当前登录用户工具类（从 Security 上下文获取 JWT 解析出的 LoginUser）
 */
public class SecurityUtils {

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return (LoginUser) auth.getPrincipal();
        }
        throw new ServiceException("请先登录");
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    public static List<String> getRoles() {
        return getLoginUser().getRoles();
    }

    public static boolean hasRole(String roleCode) {
        return getRoles().contains(roleCode);
    }

    /** 仅校医科普管理员或超级管理员可执行运维/发布操作 */
    public static boolean isDoctorOrAdmin() {
        List<String> roles = getRoles();
        return roles.contains("SUPER_ADMIN") || roles.contains("DOCTOR_ADMIN");
    }
}
