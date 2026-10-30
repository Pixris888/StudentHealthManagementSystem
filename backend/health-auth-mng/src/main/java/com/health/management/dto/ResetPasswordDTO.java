package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 管理员重置用户密码
 */
@Data
public class ResetPasswordDTO implements Serializable {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须为8-32位")
    private String newPassword;
}
