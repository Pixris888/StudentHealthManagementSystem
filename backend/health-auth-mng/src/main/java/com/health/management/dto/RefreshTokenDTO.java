package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 刷新 Access Token 请求
 */
@Data
public class RefreshTokenDTO implements Serializable {

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
