package com.health.management.controller;

import com.health.management.common.dto.LoginBody;
import com.health.management.dto.ChangePasswordDTO;
import com.health.management.dto.RefreshTokenDTO;
import com.health.management.dto.RegisterDTO;
import com.health.management.service.system.AuthService;
import com.health.management.vo.LoginVO;
import com.health.management.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证接口：登录 / 刷新 / 当前用户信息 / 退出 / 全端退出 / 修改密码
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public LoginVO login(@Valid @RequestBody LoginBody loginBody) {
        return authService.login(loginBody);
    }

    /**
     * 学生自主注册，成功即返回登录 Token（仅创建学生账号，不能指定角色）
     */
    @PostMapping("/register")
    public LoginVO register(@Valid @RequestBody RegisterDTO dto) {
        return authService.register(dto);
    }

    @PostMapping("/refresh")
    public LoginVO refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return authService.refresh(dto.getRefreshToken());
    }

    @GetMapping("/info")
    public UserInfoVO info() {
        return authService.getCurrentUserInfo();
    }

    @PostMapping("/logout")
    public boolean logout() {
        authService.logout();
        return true;
    }

    @PostMapping("/logout-all")
    public boolean logoutAll() {
        authService.logoutAll();
        return true;
    }

    @PutMapping("/password")
    public boolean changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return true;
    }
}
