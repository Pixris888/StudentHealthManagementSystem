package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 新增用户请求
 */
@Data
public class UserCreateDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须为8-32位")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickName;

    /**
     * 前端可传，服务端会根据角色重新推导
     */
    private Integer userType;

    @Size(max = 30, message = "学号长度不能超过30个字符")
    private String studentNo;

    private Integer gender;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 100, message = "院系/部门长度不能超过100个字符")
    private String deptName;

    @NotEmpty(message = "至少分配一个角色")
    private List<Long> roleIds;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
