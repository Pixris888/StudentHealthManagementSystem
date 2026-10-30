package com.health.management.service.system;

import com.health.management.common.dto.LoginBody;
import com.health.management.dto.ChangePasswordDTO;
import com.health.management.dto.RegisterDTO;
import com.health.management.vo.LoginVO;
import com.health.management.vo.UserInfoVO;

/**
 * 登录认证服务（Redis 双 Token）
 */
public interface AuthService {

    LoginVO login(LoginBody loginBody);

    /**
     * 学生自主注册并直接登录，返回与登录接口一致的 Token 信息
     */
    LoginVO register(RegisterDTO dto);

    LoginVO refresh(String refreshToken);

    UserInfoVO getCurrentUserInfo();

    void logout();

    void logoutAll();

    void changePassword(ChangePasswordDTO dto);
}
