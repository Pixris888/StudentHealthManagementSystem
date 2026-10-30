package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改用户请求（不允许修改用户名、密码、状态、用户类型）
 */
@Data
public class UserUpdateDTO implements Serializable {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickName;

    private Integer gender;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 100, message = "院系/部门长度不能超过100个字符")
    private String deptName;

    @Size(max = 30, message = "学号长度不能超过30个字符")
    private String studentNo;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
