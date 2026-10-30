package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色详情（含已授权菜单ID）
 */
@Data
public class RoleDetailVO implements Serializable {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer userType;
    private Integer roleSort;
    private Integer dataScope;
    private Integer status;
    private String remark;
    private List<Long> menuIds;
}
