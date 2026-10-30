package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("username")
    private String username; // 登录账号
    @JsonIgnore
    @TableField("password")
    private String password; // 登录密码(BCrypt加密)
    @TableField("real_name")
    private String realName; // 真实姓名
    @TableField("nick_name")
    private String nickName; // 昵称
    @TableField("user_type")
    private Integer userType; // 用户类型 1-学生 2-校医科普管理员 3-超级管理员
    @TableField("student_no")
    private String studentNo; // 学号(学生)
    @TableField("gender")
    private Integer gender; // 性别 0-未知 1-男 2-女
    @TableField("avatar")
    private String avatar; // 头像地址
    @TableField("phone")
    private String phone; // 联系电话
    @TableField("email")
    private String email; // 邮箱
    @TableField("dept_name")
    private String deptName; // 所属院系/部门
    @TableField("status")
    private Integer status; // 账号状态 0-禁用 1-启用
    @TableField("token_version")
    private Integer tokenVersion; // token版本号，改密/禁用/改角色后+1
    @TableField("login_ip")
    private String loginIp; // 最后登录IP
    @TableField("login_time")
    private LocalDateTime loginTime; // 最后登录时间
}
