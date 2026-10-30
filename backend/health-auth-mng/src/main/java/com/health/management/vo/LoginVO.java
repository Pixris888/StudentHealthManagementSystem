package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录/刷新成功返回
 */
@Data
public class LoginVO implements Serializable {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    /** Access Token 有效期（秒） */
    private Long expiresIn;

    /** Refresh Token 有效期（秒） */
    private Long refreshExpiresIn;

    private Long userId;
    private String username;
    private String realName;
    private Integer userType;
    private List<String> roles;
    private List<String> permissions;
}
