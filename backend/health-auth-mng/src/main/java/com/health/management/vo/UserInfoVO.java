package com.health.management.vo;

import com.health.management.common.vo.MenuTree;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户信息
 */
@Data
public class UserInfoVO implements Serializable {

    private Long userId;
    private String username;
    private String realName;
    private String nickName;
    private Integer userType;
    private String userTypeName;
    private String avatar;
    private String deptName;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuTree> menus;
}
