package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色列表/下拉
 */
@Data
public class RoleVO implements Serializable {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer userType;
    private Integer roleSort;
    private Integer dataScope;
    private Integer status;
    private String remark;
}
