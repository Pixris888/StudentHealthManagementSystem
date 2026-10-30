package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表/详情（不包含密码）
 */
@Data
public class UserVO implements Serializable {

    private Long id;
    private String username;
    private String realName;
    private String nickName;
    private Integer userType;
    private String userTypeName;
    private String studentNo;
    private Integer gender;
    private String avatar;
    private String phone;
    private String email;
    private String deptName;
    private Integer status;
    private String loginIp;
    private LocalDateTime loginTime;
    private LocalDateTime createTime;
    private String remark;
    private List<Long> roleIds;
    private List<RoleVO> roles;
}
